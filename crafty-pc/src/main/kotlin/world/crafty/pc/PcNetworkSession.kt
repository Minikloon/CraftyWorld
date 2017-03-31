package world.crafty.pc

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.joml.Vector3i
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.PcNetworkSession.State.*
import world.crafty.common.utils.Clock
import world.crafty.pc.proto.*
import world.crafty.pc.proto.packets.client.*
import world.crafty.pc.proto.packets.server.*
import world.crafty.common.Location
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.decompressed
import world.crafty.common.utils.hashFnv1a64
import world.crafty.common.vertx.*
import world.crafty.pc.metadata.PlayerMetadata
import world.crafty.proto.client.JoinRequestCraftyPacket
import java.io.ByteArrayInputStream
import world.crafty.pc.world.toPcPacket
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.CraftySkin
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.client.ChatFromClientCraftyPacket
import world.crafty.proto.client.ChunksRadiusRequestCraftyPacket
import world.crafty.proto.client.ReadyToSpawnCraftyPacket
import world.crafty.proto.server.*
import world.crafty.skinpool.CraftySkinPoolServer
import world.crafty.skinpool.protocol.client.HashPollPoolPacket
import world.crafty.skinpool.protocol.client.SaveSkinPoolPacket
import world.crafty.skinpool.protocol.server.HashPollReplyPoolPacket
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class PcNetworkSession(val server: PcConnectionServer, val worldServer: String, private val socket: NetSocket) {
    var state: State = HANDSHAKE
    val lastUpdate = Clock()

    val sessionId: String
    private object Common {
        val sessionIds = AtomicLong()
    }
    
    private var craftyPlayerId = 0
    
    private var location = Location(0f, 0f, 0f)

    val verifyToken = generateVerifyToken()
    var encrypted = false
    private lateinit var cipher : Cipher
    private lateinit var decipher : Cipher

    private lateinit var username : String
    private lateinit var brand : String
    
    private val eb = server.vertx.eventBus()
    
    private val compressionThreshold = 256
    private var compressing = false

    init {
        sessionId = Common.sessionIds.incrementAndGet().toString()
    }

    fun send(content: LengthPrefixedContent) {
        val buffer = Buffer.buffer(content.expectedSize)
        val bs = BufferOutputStream(buffer)
        val stream = if(encrypted) MinecraftOutputStream(CipherOutputStream(bs, cipher)) else MinecraftOutputStream(bs)
        content.serializeWithLengthPrefix(stream, compressing, compressionThreshold)
        socket.write(buffer)
    }

    fun receive(buffer: Buffer) {
        var bytes = buffer.bytes
        if(encrypted)
            bytes = decipher.update(bytes)
        if(compressing)
            compressedHandler.handle(Buffer.buffer(bytes))
        else
            uncompressedHandler.handle(Buffer.buffer(bytes))
    }

    val uncompressedHandler = LengthPrefixedHandler {
        launch(CurrentVertx) {
            val bs = ByteArrayInputStream(it.bytes)
            val stream = MinecraftInputStream(bs)
            handlePayload(stream)
        }
    }
    
    val compressedHandler = LengthPrefixedHandler {
        launch(CurrentVertx) {
            val bs = ByteArrayInputStream(it.bytes)
            val stream = MinecraftInputStream(bs)
            val decompressedLength = stream.readSignedVarInt()
            if(decompressedLength < compressionThreshold) {
                handlePayload(stream)
                return@launch
            }
            val decompressed = stream.readRemainingBytes().decompressed(CompressionAlgorithm.ZLIB, decompressedLength)
            handlePayload(MinecraftInputStream(decompressed))
        }
    }
    
    suspend private fun handlePayload(stream: MinecraftInputStream) {
        val packetId = stream.readSignedVarInt()

        val codec = state.pcPacketList.idToCodec[packetId]
        if(codec == null) {
            println("Unknown packet id $packetId while in state $state")
            return
        }
        val packet = codec.deserialize(stream)
        if(packet !is ClientKeepAlivePcPacket && packet !is PlayerPositionPcPacket && packet !is PlayerPosAndLookPcPacket && packet !is PlayerLookPcPacket)
            println("received ${packet.javaClass.simpleName}")

        when (state) {
            HANDSHAKE -> handleHandshakePacket(packet)
            STATUS -> handleStatusPacket(packet)
            LOGIN -> handleLoginPacket(packet)
            PLAY -> handlePlayPacket(packet)
        }
        lastUpdate.reset()
    }

    private fun handleHandshakePacket(packet: PcPacket) {
        when (packet) {
            is HandshakePcPacket -> {
                println("Received handshake from ${socket.remoteAddress()}")
                when(packet.nextState) {
                    1 -> state = STATUS
                    2 -> state = LOGIN
                }
            }
            else -> {
                println("Unhandled Handshake packet ${packet.javaClass.simpleName}")
            }
        }
    }

    private fun handleStatusPacket(packet: PcPacket) {
        when (packet) {
            is StatusRequestPcPacket -> {
                println("Received server list request from ${socket.remoteAddress()}")
                
                val lpr = StatusResponsePcPacket(
                        StatusResponsePcPacket.ServerVersion(
                                name = "§l1.11.2+",
                                protocol = 316
                        ), StatusResponsePcPacket.PlayerStatus(
                        max = 1337,
                        online = 1336
                        ),StatusResponsePcPacket.Description(
                            text = "first line \nsecond line"
                        )
                )

                send(lpr)
            }
            is PingPcPacket -> {
                send(PongPcPacket(packet.epoch))
            }
            else -> {
                println("Unhandled Status packet ${packet.javaClass.simpleName}")
            }
        }
    }

    private suspend fun handleLoginPacket(packet: PcPacket) {
        when (packet) {
            is LoginStartPcPacket -> {
                username = packet.username
                println("Login start from ${packet.username}")
                send(EncryptionRequestPcPacket(sessionId, server.x509PubKey, verifyToken))
            }
            is EncryptionResponsePcPacket -> {
                if(! Arrays.equals(server.decipher.doFinal(packet.verifyToken), verifyToken)) {
                    println("Session ${socket.remoteAddress()} sent the wrong verification token")
                    socket.close()
                    return
                }
                val sharedSecret = SecretKeySpec(server.decipher.doFinal(packet.sharedSecret), "AES")
                cipher = createCipher(Cipher.ENCRYPT_MODE, sharedSecret)
                decipher = createCipher(Cipher.DECRYPT_MODE, sharedSecret)
                encrypted = true
                println("Now encrypting with ${socket.remoteAddress()}")

                send(SetCompressionPcPacket(compressionThreshold))
                compressing = true

                val serverId = server.mojang.getServerIdHash(sessionId, sharedSecret, server.x509PubKey)
                val profile = server.mojang.checkHasJoinedAsync(username, serverId)
                
                val textureProp = profile.properties.first { it.name == "textures" } // TODO: cache pc skins in pool for faster login
                val texturePropValue = Base64.getDecoder().decode(textureProp.value)
                val textureJson = JsonObject(texturePropValue.toString(Charsets.UTF_8)).getJsonObject("textures")
                val skinJson = textureJson.getJsonObject("SKIN")
                val isSkinSlim = skinJson.getJsonObject("metadata") != null
                val skinUrl = skinJson.getString("url")
                val httpClient = WebClient.create(server.vertx)
                val response = vxHttp { httpClient.getAbs(skinUrl).send(it) }
                if(response.statusCode() != HttpResponseStatus.OK.code()) {
                   throw IllegalStateException("Couldn't download pc skin for $username at $skinUrl")
                }
                val skinPngBytes = response.body().bytes
                val skinHash = hashFnv1a64(skinPngBytes)
                val skin = CraftySkin(skinHash, skinPngBytes)

                val skinPollReply = eb.typedSendAsync<HashPollReplyPoolPacket>(CraftySkinPoolServer.channelPrefix, HashPollPoolPacket(skinHash, false)).body()
                if(! skinPollReply.hasProfile) {
                    eb.typedSend(CraftySkinPoolServer.channelPrefix, SaveSkinPoolPacket(skinHash, isSkinSlim, skinPngBytes))
                }
                
                val craftyResponse = vxm<JoinResponseCraftyPacket> { eb.send("$worldServer:join", JoinRequestCraftyPacket(username, true, false, MinecraftPlatform.PC, skin), it) }
                
                if(!craftyResponse.accepted)
                    throw IllegalStateException("Crafty refused us from joining") // TODO: handle failure
                
                craftyPlayerId = craftyResponse.playerId!!
                
                send(LoginSuccessPcPacket(profile.uuid, profile.name))
                state = PLAY

                eb.consumer<ChatMessageCraftyPacket>("p:c:$craftyPlayerId:chat") {
                    val text = it.body().text
                    send(ServerChatMessagePcPacket(McChat(text), ChatPosition.CHAT_BOX))
                }

                val prespawn = craftyResponse.prespawn!!
                location = prespawn.spawnLocation
                send(JoinGamePcPacket(
                        eid = prespawn.entityId,
                        gamemode = prespawn.gamemode,
                        dimension = prespawn.dimension,
                        difficulty = 0,
                        maxPlayers = 0,
                        levelType = "flat",
                        reducedDebug = false
                ))
                send(ServerPluginMessagePcPacket("MC|Brand", server.encodedBrand))
                
                eb.send<ChunksRadiusResponseCraftyPacket>("p:s:$craftyPlayerId:chunkRadiusReq", ChunksRadiusRequestCraftyPacket(prespawn.spawnLocation.positionVec3(), 0, 8)) {
                    val response = it.result().body()
                    val cache = server.getWorldCache(worldServer)
                    response.chunkColumns.forEach { setColumn ->
                        val chunkPacket = when(setColumn.cacheStrategy) {
                            ChunkCacheStategy.PER_WORLD -> cache.getOrComputeChunkPacket(setColumn, CraftyChunkColumn::toPcPacket)
                            ChunkCacheStategy.PER_PLAYER -> setColumn.chunkColumn.toPcPacket()
                        }
                        send(chunkPacket)
                    }

                    eb.typedConsumer("p:c:$craftyPlayerId", UpdatePlayerListCraftyPacket::class) {
                        launch(CurrentVertx) {
                            handleUpdatePlayerList(it)
                        }
                    }

                    eb.typedConsumer("p:c:$craftyPlayerId", SpawnSelfCraftyPacket::class) {
                        send(SetSpawnPositionPcPacket(Vector3i(location.x.toInt(), location.y.toInt(), location.z.toInt())))
                        send(TeleportPlayerPcPacket(PcLocation(location), 0, 1))
                    }
                    
                    eb.typedSend("p:s:$craftyPlayerId", ReadyToSpawnCraftyPacket())

                    val playerMetadata = PlayerMetadata(
                            status = 0,
                            air = 0,
                            name = "",
                            nameVisible = false,
                            silent = false,
                            gravity = true,
                            handStatus = 0b01,
                            health = 20f,
                            potionEffectColor = 0,
                            potionEffectAmbient = false,
                            insertedArrows = 3,
                            extraHearts = 0,
                            score = 0,
                            mainHand = 0,
                            skinParts = 0b1111111
                    )

                    /*
                server.sessions.values.forEach {
                    it.send(PlayerListItemPcPacket(0, listOf(
                            PlayerListItemPcPacket.PlayerListPcAdd(
                                    uuid = profile.uuid,
                                    name = profile.name,
                                    properties = profile.properties,
                                    gamemode = 1,
                                    ping = 30
                            )
                    )))
                    if(it != this) {
                        it.send(SpawnPlayerPcPacket(
                                4,
                                profile.uuid,
                                PcLocation(spawnPos),
                                playerMetadata)
                        )
                    }
                }
                */
                }
            }
            else -> {
                println("Unhandled Login packet ${packet.javaClass.simpleName}")
            }
        }
    }
    
    suspend private fun handleUpdatePlayerList(message: Message<UpdatePlayerListCraftyPacket>) { // TODO: move this out of the session
        val byItemType = message.body().items.groupBy { it.type }
        byItemType.forEach {
            val itemType = it.key
            val craftyItems = it.value
            
            val action: Int
            val pcItems = mutableListOf<PlayerListPcItem>()
            
            when(itemType) {
                PlayerListItemType.ADD -> {
                    action = PlayerListPcAdd.Codec.action
                    
                    class getUuidProfile(val craftyItem: PlayerListAdd, val getProfile: Deferred<Message<HashPollReplyPoolPacket>>)
                    craftyItems.map {
                        val add = it as PlayerListAdd
                        val req = HashPollPoolPacket(add.skin.fnvHashOfPng, true)
                        val getProfile = async(CurrentVertx) {
                            eb.typedSendAsync<HashPollReplyPoolPacket>(CraftySkinPoolServer.channelPrefix, req)
                        }
                        getUuidProfile(add, getProfile)
                    }.forEach {
                        val craftyItem = it.craftyItem
                        val textureProp = it.getProfile.await().body().textureProp
                        val playerProps = if(textureProp == null) emptyList() else listOf(textureProp)
                        println("pc add to player list ${craftyItem.uuid} ${craftyItem.name}")
                        pcItems.add(PlayerListPcAdd(
                                uuid = craftyItem.uuid,
                                name = craftyItem.name,
                                properties = playerProps,
                                gamemode = 0,
                                ping = 50,
                                displayName = null
                        ))
                    }                    
                }
                PlayerListItemType.REMOVE -> {
                    action = PlayerListPcRemove.Codec.action
                    pcItems.addAll(craftyItems.map {
                        PlayerListPcRemove(
                                uuid = it.uuid
                        )
                    })
                }
            }
            
            send(PlayerListItemPcPacket(
                    action = action,
                    items = pcItems
            ))
        }
    }

    private fun createCipher(mode: Int, secret: SecretKey) : Cipher {
        val cipher = Cipher.getInstance("AES/CFB8/NoPadding")
        cipher.init(mode, secret, IvParameterSpec(secret.encoded))
        return cipher
    }

    private lateinit var loc: Location
    private fun handlePlayPacket(packet: PcPacket) {
        when (packet) {
            is ClientSettingsPcPacket -> {
                println("Client settings like locale ${packet.locale}")
            }
            is ClientKeepAlivePcPacket -> {

            }
            is ClientPluginMessagePcPacket -> {
                val channel = packet.channel
                val dataStream = MinecraftInputStream(packet.data)
                when(channel) {
                    "MC|Brand" -> {
                        brand = dataStream.readSignedString()
                    }
                    else -> {
                        println("Unhandled packet on channel $channel with ${packet.data.size} bytes")
                    }
                }
            }
            is ClientChatMessagePcPacket -> {
                eb.send("p:s:$craftyPlayerId:chat", ChatFromClientCraftyPacket(packet.text))
            }
            is PlayerPositionPcPacket -> {
                server.sessions.values.forEach {
                    if(it != this) {
                        it.send(TeleportEntityPcPacket(4, packet.x, packet.y, packet.z, 0, 0, packet.onGround))
                    }
                }
            }
            is PlayerPosAndLookPcPacket -> {

            }
            is PlayerLookPcPacket -> {

            }
            else -> {
                println("Unhandled Play packet ${packet.javaClass.simpleName}")
            }
        }
    }

    enum class State(val pcPacketList: InboundPcPacketList) {
        HANDSHAKE(ServerBoundPcHandshakePackets),
        STATUS(ServerBoundPcStatusPackets),
        LOGIN(ServerBoundPcLoginPackets),
        PLAY(ServerBoundPcPlayPackets),
        ;
    }

    enum class MinecraftConnPipeSteps(val order: Int) {
        ENCRYPTION(1),
        COMPRESSION(2)
        ;
    }
}