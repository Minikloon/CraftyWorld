package club.kazza.kazzacraft.network

import club.kazza.kazzacraft.Location
import club.kazza.kazzacraft.metadata.PlayerMetadata
import club.kazza.kazzacraft.utils.LongPackedArray
import club.kazza.kazzacraft.network.protocol.*
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import club.kazza.kazzacraft.network.PcNetworkSession.State.*
import club.kazza.kazzacraft.network.security.generateVerifyToken
import club.kazza.kazzacraft.network.serialization.LengthPrefixedHandler
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import club.kazza.kazzacraft.utils.Clock
import club.kazza.kazzacraft.world.ChunkColumn
import club.kazza.kazzacraft.world.ChunkSection
import club.kazza.kazzacraft.world.Dimension
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class PcNetworkSession(val server: PcConnectionServer, private val socket: NetSocket) {
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

    init {
        sessionId = Common.sessionIds.incrementAndGet().toString()
        socket.handler({ receive(it) })
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
            mcPacketStream.writePacket(packet)
        } else {
            val mcPacketStream = MinecraftOutputStream(packetStream)
            mcPacketStream.writePacket(packet)
        }

        val bytes = packetStream.toByteArray()
        socket.write(Buffer.buffer(bytes))
    }

    val uncompressedHandler = LengthPrefixedHandler() {
        val stream = ByteArrayInputStream(it.bytes)
        val reader = MinecraftInputStream(stream)

        val packetId = reader.readVarInt()

        val codec = state.pcPacketList.idToCodec[packetId]
        if(codec == null) {
            println("Unknown packet id $packetId while in state $state")
            return@LengthPrefixedHandler
        }

        val packet = codec.deserialize(stream)
        println("received ${packet.javaClass.simpleName}")

        when(state) {
            HANDSHAKE -> handleHandshakePacket(packet)
            STATUS -> handleStatusPacket(packet)
            LOGIN -> handleLoginPacket(packet)
            PLAY -> handlePlayPacket(packet)
        }

        lastUpdate.reset()
    }

    private fun handleHandshakePacket(packet: PcPacket) {
        when (packet) {
            is Pc.Client.Handshake.HandshakePcPacket -> {
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
            is Pc.Client.Status.RequestPcPacket -> {
                println("Received server list request from ${socket.remoteAddress()}")

                val lpr = Pc.Server.Status.ResponsePcPacket(
                        Pc.Server.Status.ResponsePcPacket.ServerVersion(
                                name = "1.10",
                                protocol = 210
                        ), Pc.Server.Status.ResponsePcPacket.PlayerStatus(
                        max = 1337,
                        online = 1336
                ), Pc.Server.Status.ResponsePcPacket.Description(
                        text = "Minicraft Servah"
                )
                )

                send(lpr)
            }
            is Pc.Client.Status.PingPcPacket -> {
                send(Pc.Server.Status.PongPcPacket(packet.epoch))
            }
            else -> {
                println("Unhandled Status packet ${packet.javaClass.simpleName}")
            }
        }
    }

    private fun handleLoginPacket(packet: PcPacket) {
        when (packet) {
            is Pc.Client.Login.LoginStartPcPacket -> {
                username = packet.username
                println("Login start from ${packet.username}")
                send(Pc.Server.Login.EncryptionRequestPcPacket(sessionId, server.x509PubKey, verifyToken))
            }
            is Pc.Client.Login.EncryptionResponsePcPacket -> {
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
                server.mojang.checkHasJoined(username, serverId, Handler {
                    if(it.succeeded()) {
                        println("mojang replied to $username auth")
                        val profile = it.result()
                        state = PLAY
                        send(Pc.Server.Login.LoginSuccessPcPacket(profile.uuid, profile.name))
                        send(Pc.Server.Play.JoinGamePcPacket(3, 1, 0, 0, 20, "flat", false))
                        send(Pc.Server.Play.ServerPluginMessagePcPacket("MC|Brand", server.encodedBrand))

                        val world = server.world
                        val spawnPos = world.spawn

                        loc = spawnPos

                        send(Pc.Server.Play.SpawnPositionPcPacket(spawnPos))

                        val toSend = world.chunks.filter { (it.x - spawnPos.x/16) * (it.x - spawnPos.x/16) + (it.z - spawnPos.z/16) * (it.z - spawnPos.z/16) < 8*8 }
                        toSend.map { it.toPacket() }.forEach { send(it) }

                        send(Pc.Server.Play.ServerChatMessage(McChat("Welcome!"), 0))
                        send(Pc.Server.Play.PlayerListHeaderFooterPcPacket(McChat("Header"), McChat("Footer")))

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
                            it.send(Pc.Server.Play.PlayerListItemPcPacket(0, listOf(
                                    Pc.Server.Play.PlayerListItemPcPacket.PlayerListItemAdd(
                                            uuid = profile.uuid,
                                            name = profile.name,
                                            properties = profile.properties,
                                            gamemode = 1,
                                            ping = 30
                                    )
                            )))
                            if(it != this) {
                                it.send(Pc.Server.Play.SpawnPlayerPcPacket(
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

                        send(Pc.Server.Play.PlayerTeleportPcPacket(spawnPos, 0, 1))
                    } else {
                        it.cause().printStackTrace()
                    }
                })
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
            is Pc.Client.Play.ClientSettingsPcPacket -> {
                println("Client settings like locale ${packet.locale}")
            }
            is Pc.Client.Play.ClientKeepAlivePcPacket -> {

            }
            is Pc.Client.Play.ClientPluginMessagePcPacket -> {
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
            is Pc.Client.Play.ClientChatMessagePcPacket -> {
                val chat = Pc.Server.Play.ServerChatMessage(McChat("<$username> ${packet.message}"), 0)
                server.sessions.values
                        .filter { it.state == PLAY }
                        .forEach { it.send(chat) }
            }
            is Pc.Client.Play.ClientPlayerPositionPcPacket -> {
                server.sessions.values.forEach {
                    if(it != this) {
                        it.send(Pc.Server.Play.EntityTeleportPcPacket(4, packet.x, packet.y, packet.z, 0, 0, packet.onGround))
                    }
                }
            }
            is Pc.Client.Play.ClientPlayerPosAndLookPcPacket -> {

            }
            is Pc.Client.Play.ClientPlayerLookPcPacket -> {

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