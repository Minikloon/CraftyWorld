package world.crafty.pc

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.joml.Vector3i
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.PcNetworkSession.State.*
import world.crafty.common.utils.Clock
import world.crafty.pc.proto.*
import world.crafty.pc.proto.packets.client.*
import world.crafty.pc.proto.packets.server.*
import world.crafty.pc.world.Location
import world.crafty.pc.metadata.PlayerMetadata
import world.crafty.proto.client.JoinRequestCraftyPacket
import world.crafty.proto.server.JoinResponseCraftyPacket
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import world.crafty.common.utils.sinceThen
import world.crafty.common.vertx.VertxContext
import world.crafty.common.vertx.vx
import world.crafty.common.vertx.vxm
import java.time.Instant
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

    val verifyToken = generateVerifyToken()
    var encrypted = false
    private lateinit var cipher : Cipher
    private lateinit var decipher : Cipher

    private lateinit var username : String
    private lateinit var brand : String
    
    private val eb = server.vertx.eventBus()

    init {
        sessionId = Common.sessionIds.incrementAndGet().toString()
    }

    fun receive(buffer: Buffer) {
        var bytes = buffer.bytes
        if(encrypted)
            bytes = decipher.update(bytes)
        uncompressedHandler.handle(Buffer.buffer(bytes))
    }

    fun send(packet: PcPacket) {
        val packetStream = ByteArrayOutputStream()
        if(encrypted) {
            val cipherStream = CipherOutputStream(packetStream, cipher)
            val mcPacketStream = MinecraftOutputStream(cipherStream)
            packet.serializeWithHeader(mcPacketStream)
        } else {
            val mcPacketStream = MinecraftOutputStream(packetStream)
            packet.serializeWithHeader(mcPacketStream)
        }

        val bytes = packetStream.toByteArray()
        socket.write(Buffer.buffer(bytes))
    }

    val uncompressedHandler = LengthPrefixedHandler {
        launch(VertxContext) {
            val stream = ByteArrayInputStream(it.bytes)
            val reader = MinecraftInputStream(stream)

            val packetId = reader.readUnsignedVarInt()

            val codec = state.pcPacketList.idToCodec[packetId]
            if(codec == null) {
                println("Unknown packet id $packetId while in state $state")
                return@launch
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

                val serverId = server.mojang.getServerIdHash(sessionId, sharedSecret, server.x509PubKey)
                val profile = server.mojang.checkHasJoinedAsync(username, serverId)
                
                println("mojang replied to $username auth")
                
                val craftyResponse = vxm<JoinResponseCraftyPacket> { eb.send("$worldServer:join", JoinRequestCraftyPacket(username, true, false), it) }

                if(!craftyResponse.accepted)
                    throw IllegalStateException("Crafty refused us from joining") // TODO: handle failure
                
                send(LoginSuccessPcPacket(profile.uuid, profile.name))
                state = PLAY
                
                send(JoinGamePcPacket(3, 1, 0, 0, 20, "flat", false))
                send(ServerPluginMessagePcPacket("MC|Brand", server.encodedBrand))

                val world = server.world
                val spawnPos = world.spawn

                loc = spawnPos

                send(SetSpawnPositionPcPacket(Vector3i(spawnPos.x.toInt(), spawnPos.y.toInt(), spawnPos.z.toInt())))

                val toSend = world.chunks.filter { (it.x - spawnPos.x/16) * (it.x - spawnPos.x/16) + (it.z - spawnPos.z/16) * (it.z - spawnPos.z/16) < 8*8 }
                toSend.map { it.toPacket() }.forEach { send(it) }

                send(ServerChatMessagePcPacket(McChat("Welcome!"), ChatPosition.CHAT_BOX))
                send(PlayerListHeaderFooterPcPacket(McChat("Header"), McChat("Footer")))

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

                server.sessions.values.forEach {
                    it.send(PlayerListItemPcPacket(0, listOf(
                            PlayerListItemPcPacket.PlayerListItemAdd(
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
                                spawnPos.x,
                                spawnPos.y,
                                spawnPos.z,
                                0,
                                0,
                                playerMetadata)
                        )
                    }
                }

                send(TeleportPlayerPcPacket(spawnPos, 0, 1))
            }
            else -> {
                println("Unhandled Login packet ${packet.javaClass.simpleName}")
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
                println("Client settings like locale ${packet.locale}")
            }
            is ClientKeepAlivePcPacket -> {

            }
            is ClientPluginMessagePcPacket -> {
                val channel = packet.channel
                val dataStream = MinecraftInputStream(packet.data)
                when(channel) {
                    "MC|Brand" -> {
                        brand = dataStream.readString()
                    }
                    else -> {
                        println("Unhandled packet on channel $channel with ${packet.data.size} bytes")
                    }
                }
            }
            is ClientChatMessagePcPacket -> {
                val chat = ServerChatMessagePcPacket(McChat("<$username> ${packet.message}"), ChatPosition.CHAT_BOX)
                server.sessions.values
                        .filter { it.state == PLAY }
                        .forEach { it.send(chat) }
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