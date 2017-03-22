package world.crafty.pc

import io.vertx.core.AbstractVerticle
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.packets.server.ServerKeepAlivePcPacket
import world.crafty.pc.mojang.MojangClient
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher

class PcConnectionServer(val port: Int, val worldServer: String) : AbstractVerticle() {
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
        mcStream.writeSignedString("crafty")
        encodedBrand = brandStream.toByteArray()
    }

    override fun start() {
        mojang = MojangClient(vertx)

        server = vertx.createNetServer()
        server.connectHandler {
            val session = PcNetworkSession(this, worldServer, it)
            sessions[it] = session
            it.handler({ session.receive(it) })
            println("Received PC connection from ${it.remoteAddress()}")
        }
        server.listen(port)

        vertx.setPeriodic(1000) {
            sessions.values.filter { it.state == PcNetworkSession.State.PLAY }.forEach { session ->
                session.send(ServerKeepAlivePcPacket(0))
            }
        }

        vertx.setPeriodic(1000) {
            val toRemove = sessions.filter { it.value.lastUpdate.elapsed.seconds > 3 }
            for(key in toRemove.keys)
                sessions.remove(key)
        }
    }
}