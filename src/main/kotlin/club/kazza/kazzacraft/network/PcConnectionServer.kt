package club.kazza.kazzacraft.network

import io.vertx.core.AbstractVerticle
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import club.kazza.kazzacraft.network.mojang.MojangClient
import club.kazza.kazzacraft.network.protocol.Pc
import club.kazza.kazzacraft.network.security.convertKeyToX509
import club.kazza.kazzacraft.network.security.createDecipher
import club.kazza.kazzacraft.network.security.generateKeyPair
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import club.kazza.kazzacraft.world.World
import java.io.ByteArrayOutputStream
import java.time.Duration
import javax.crypto.Cipher

class PcConnectionServer(val port: Int, val world: World) : AbstractVerticle() {
    lateinit var server: NetServer
    val sessions = mutableMapOf<NetSocket, PcNetworkSession>()

    lateinit var mojang: MojangClient

    val decipher: Cipher
    val x509PubKey: ByteArray
    val encodedBrand: ByteArray

    init {
        val rsaKey = generateKeyPair()
        x509PubKey = convertKeyToX509(rsaKey.public).encoded
        decipher = createDecipher(rsaKey)

        val brandStream = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(brandStream)
        mcStream.writeString("snowfite")
        encodedBrand = brandStream.toByteArray()
    }

    override fun start() {
        mojang = MojangClient(vertx)

        server = vertx.createNetServer()
        server.connectHandler() {
            val session = PcNetworkSession(this, it)
            sessions[it] = session
            println("Received PC connection from ${it.remoteAddress()}")
        }
        server.listen(port)

        vertx.setPeriodic(1000) {
            for(session in sessions.values.filter { it.state == PcNetworkSession.State.PLAY })
                session.send(Pc.Server.Play.ServerKeepAlivePcPacket(0))
        }

        vertx.setPeriodic(1000) {
            val toRemove = sessions.filter { it.value.lastUpdate.elapsed.seconds > 3500 }
            for(key in toRemove.keys)
                sessions.remove(key)
        }
    }
}