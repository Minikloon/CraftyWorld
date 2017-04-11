package world.crafty.pc

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.joml.Vector3f
import world.crafty.common.Angle256
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.PcNetworkSession.State.*
import world.crafty.pc.proto.*
import world.crafty.pc.proto.packets.client.*
import world.crafty.pc.proto.packets.server.*
import world.crafty.common.Location
import world.crafty.common.utils.*
import world.crafty.common.vertx.*
import world.crafty.mojang.MojangProfile
import world.crafty.pc.entity.PcEntity
import java.io.ByteArrayInputStream
import world.crafty.pc.world.toPcPacket
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.CraftySkin
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.packets.client.*
import world.crafty.proto.packets.server.*
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

private val log = getLogger<PcNetworkSession>()
class PcNetworkSession(val server: PcConnectionServer, val worldServer: String, private val socket: NetSocket) {
    var state: State = HANDSHAKE
    val lastUpdate = Clock()

    val sessionId: String
    private object Common {
        val sessionIds = AtomicLong()
    }
    
    private var craftyPlayerId = 0
    
    private var ownEntityId = 0
    private var location = Location(0f, 0f, 0f)
    private val loadedEntities = mutableMapOf<Int, PcEntity>()

    val verifyToken = generateVerifyToken()
    var encrypted = false
    private lateinit var cipher : Cipher
    private lateinit var decipher : Cipher

    private lateinit var username : String
    private lateinit var profile: MojangProfile
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
            log.error { "${System.currentTimeMillis()} Unknown pc packet id $packetId while in state $state" }
            return
        }
        val packet = codec.deserialize(stream)
        if(packet !is ClientKeepAlivePcPacket && packet !is PlayerPositionPcPacket && packet !is PlayerPosAndLookPcPacket && packet !is PlayerLookPcPacket)
            log.trace { "received ${packet.javaClass.simpleName}" }

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
                log.info { "Received handshake from ${socket.remoteAddress()}" }
                when(packet.nextState) {
                    1 -> state = STATUS
                    2 -> state = LOGIN
                }
            }
            else -> {
                log.error { "Unhandled Handshake packet ${packet.javaClass.simpleName}" }
            }
        }
    }

    private fun handleStatusPacket(packet: PcPacket) {
        when (packet) {
            is StatusRequestPcPacket -> {
                log.info { "Received server list request from ${socket.remoteAddress()}" }
                
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
                log.error { "Unhandled Status packet ${packet.javaClass.simpleName}" }
            }
        }
    }

    private suspend fun handleLoginPacket(packet: PcPacket) {
        when (packet) {
            is LoginStartPcPacket -> {
                username = packet.username
                log.info { "Login start from ${packet.username}" }
                send(EncryptionRequestPcPacket(sessionId, server.x509PubKey, verifyToken))
            }
            is EncryptionResponsePcPacket -> {
                if(! Arrays.equals(server.decipher.doFinal(packet.verifyToken), verifyToken)) {
                    log.warn { "Session ${socket.remoteAddress()} sent the wrong verification token" }
                    socket.close()
                    return
                }
                val sharedSecret = SecretKeySpec(server.decipher.doFinal(packet.sharedSecret), "AES")
                cipher = createCipher(Cipher.ENCRYPT_MODE, sharedSecret)
                decipher = createCipher(Cipher.DECRYPT_MODE, sharedSecret)
                encrypted = true
                log.info { "Now encrypting with ${socket.remoteAddress()}" }

                send(SetCompressionPcPacket(compressionThreshold))
                compressing = true

                val serverId = server.mojang.getServerIdHash(sessionId, sharedSecret, server.x509PubKey)
                profile = server.mojang.checkHasJoinedAsync(username, serverId)
                
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
                ownEntityId = prespawn.entityId
                location = prespawn.spawnLocation
                send(JoinGamePcPacket(
                        eid = prespawn.entityId.toInt(),
                        gamemode = prespawn.gamemode,
                        dimension = prespawn.dimension,
                        difficulty = 0,
                        maxPlayers = 0,
                        levelType = "flat",
                        reducedDebug = false
                ))
                send(ServerPluginMessagePcPacket("MC|Brand", server.encodedBrand))
                
                eb.typedSend<ChunksRadiusResponseCraftyPacket>("p:s:$craftyPlayerId", ChunksRadiusRequestCraftyPacket(prespawn.spawnLocation.positionVec3(), 0, 8)) {
                    val response = it.result().body()
                    val cache = server.getWorldCache(worldServer)
                    response.chunkColumns.forEach { setColumn ->
                        val chunkPacket = when(setColumn.cacheStrategy) {
                            ChunkCacheStategy.PER_WORLD -> cache.getOrComputeChunkPacket(setColumn, CraftyChunkColumn::toPcPacket)
                            ChunkCacheStategy.PER_PLAYER -> setColumn.chunkColumn.toPcPacket()
                        }
                        send(chunkPacket)
                    }
                    
                    eb.typedConsumer("p:c:$craftyPlayerId", AddPlayerCraftyPacket::class) {
                        val packet = it.body()
                        
                        if(packet.craftyId == craftyPlayerId)
                            return@typedConsumer
                        
                        launch(CurrentVertx) {
                            val skinReq = HashPollPoolPacket(packet.skin.fnvHashOfPng, needProfile = true)
                            val skinProfile = eb.typedSendAsync<HashPollReplyPoolPacket>(CraftySkinPoolServer.channelPrefix, skinReq).body()
                            val playerProps = if(skinProfile.textureProp == null) emptyList() else listOf(skinProfile.textureProp!!)

                            val entity = PcEntity(packet.entityId, packet.location)
                            loadedEntities[entity.id] = entity

                            if(packet.craftyId == craftyPlayerId) // TODO: fix hack
                                return@launch

                            val metadata = entity.metaFromCrafty(server.metaTranslatorRegistry, packet.meta)
                            
                            send(PlayerListItemPcPacket(
                                    action = PlayerListAction.ADD,
                                    items = listOf(
                                            PlayerListPcAdd(
                                                    uuid = packet.uuid,
                                                    name = packet.username,
                                                    properties = playerProps,
                                                    gamemode = 0,
                                                    ping = 30,
                                                    displayName = null
                                            )
                                    )
                            ))
                            send(SpawnPlayerPcPacket(
                                    entityId = packet.entityId,
                                    uuid = packet.uuid,
                                    location = packet.location,
                                    metadata = metadata
                            ))
                            send(SetEntityHeadLookPcPacket(
                                    entityId = packet.entityId.toInt(),
                                    headYaw = packet.location.headYaw
                            ))
                            delay(1000)
                            send(PlayerListItemPcPacket(PlayerListAction.REMOVE, listOf(PlayerListPcRemove(packet.uuid))))
                        }
                    }
                    
                    eb.typedConsumer("p:c:$craftyPlayerId", PatchEntityCraftyPacket::class) {
                        val packet = it.body()
                        val entity = loadedEntities[packet.entityId.toInt()] ?: return@typedConsumer
                        val meta = entity.metaFromCrafty(server.metaTranslatorRegistry, packet.values)
                        send(EntityMetadataPcPacket(packet.entityId.toInt(), meta))
                    }
                    
                    eb.typedConsumer("p:c:$craftyPlayerId", SetEntityLocationCraftyPacket::class) {
                        val packet = it.body()
                        if(packet.entityId == ownEntityId) return@typedConsumer
                        
                        val entity = loadedEntities[packet.entityId.toInt()] ?: return@typedConsumer
                        
                        entity.getMovePacketsAndClear(packet.location, packet.onGround).forEach {
                            send(it)
                        }
                    }

                    eb.typedConsumer("p:c:$craftyPlayerId", SpawnSelfCraftyPacket::class) {
                        launch(CurrentVertx) {
                            send(PlayerListItemPcPacket(
                                    action = PlayerListAction.ADD,
                                    items = listOf(
                                            PlayerListPcAdd(
                                                    uuid = profile.uuid,
                                                    name = profile.name,
                                                    properties = profile.properties,
                                                    gamemode = 0,
                                                    ping = 30,
                                                    displayName = null
                                            )
                                    )
                            ))
                            send(TeleportPlayerPcPacket(location, 0, 1))
                            delay(1000)
                            send(PlayerListItemPcPacket(
                                    action = PlayerListAction.REMOVE,
                                    items = listOf(PlayerListPcRemove(profile.uuid))
                            ))
                        }
                    }
                    
                    eb.typedSend("p:s:$craftyPlayerId", ReadyToSpawnCraftyPacket())
                }
            }
            else -> {
                log.error { "Unhandled Login packet ${packet.javaClass.simpleName}" }
            }
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
                log.info { "Client settings like locale ${packet.locale}" }
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
                        log.warn { "Unhandled packet on channel $channel with ${packet.data.size} bytes" }
                    }
                }
            }
            is ClientChatMessagePcPacket -> {
                eb.typedSend("p:s:$craftyPlayerId", ChatFromClientCraftyPacket(packet.text))
            }
            is PlayerPositionPcPacket -> {
                val pos = Vector3f(packet.x.toFloat(), packet.y.toFloat(), packet.z.toFloat())
                eb.typedSend("p:s:$craftyPlayerId", SetPlayerPosCraftyPacket(pos, packet.onGround))
            }
            is PlayerPosAndLookPcPacket -> {
                val pos = Location(
                        x = packet.x.toFloat(),
                        y = packet.y.toFloat(),
                        z = packet.z.toFloat(),
                        bodyYaw = Angle256.fromDegrees(packet.yaw),
                        headYaw = Angle256.fromDegrees(packet.yaw),
                        headPitch = Angle256.fromDegrees(packet.pitch)
                )
                eb.typedSend("p:s:$craftyPlayerId", SetPlayerPosAndLookCraftyPacket(pos, packet.onGround))
            }
            is PlayerLookPcPacket -> {
                val headPitch = Angle256.fromDegrees(packet.pitch)
                val headYaw = Angle256.fromDegrees(packet.yaw)
                val bodyYaw = headYaw
                eb.typedSend("p:s:$craftyPlayerId", SetPlayerLookCraftyPacket(headPitch, headYaw, bodyYaw))
            }
            is EntityActionPcPacket -> {
                val craftyAction = when(packet.action) {
                    PcEntityAction.START_SNEAKING -> PlayerAction.START_SNEAK
                    PcEntityAction.STOP_SNEAKING -> PlayerAction.STOP_SNEAK
                    PcEntityAction.LEAVE_BED -> PlayerAction.LEAVE_BED
                    PcEntityAction.START_SPRINTING -> PlayerAction.START_SPRINT
                    PcEntityAction.STOP_SPRINTING -> PlayerAction.STOP_SNEAK
                    PcEntityAction.START_HORSE_JUMP -> return
                    PcEntityAction.STOP_HORSE_JUMP -> return
                    PcEntityAction.OPEN_HORSE_INVENTORY -> return
                    PcEntityAction.START_ELYTRA_FLY -> return
                }
                eb.typedSend("p:s:$craftyPlayerId", PlayerActionCraftyPacket(craftyAction))
            }
            else -> {
                log.error { "Unhandled Play packet ${packet.javaClass.simpleName}" }
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