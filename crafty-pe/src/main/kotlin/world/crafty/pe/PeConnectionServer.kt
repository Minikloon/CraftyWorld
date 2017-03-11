package world.crafty.pe

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.AbstractVerticle
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.json.Json
import io.vertx.core.net.SocketAddress
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.CompletableFuture

class PeConnectionServer(val port: Int) : AbstractVerticle() {
    lateinit var socket: DatagramSocket
    val sessions: MutableMap<SocketAddress, CompletableFuture<PeNetworkSession>> = mutableMapOf()
    val supportsEncryption = false
    
    val keyPair = {
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(ECGenParameterSpec("secp384r1"))
        keyGen.generateKeyPair()
    }()

    override fun start() {
        Json.mapper.registerKotlinModule()
        Json.mapper.deserializationConfig
        
        socket = vertx.createDatagramSocket()
        socket.listen(port, "0.0.0.0") {
            if(it.succeeded()) {
                socket.handler { datagram ->
                    val sender = datagram.sender()
                    val getSession = sessions.getOrPut(sender) {
                        val future = CompletableFuture<PeNetworkSession>()
                        val session = PeNetworkSession(this, socket, sender)
                        vertx.deployVerticle(session) {
                            future.complete(session)
                        }
                        future
                    }
                    getSession.thenAcceptAsync {
                        val session = it
                        session.queueReceivedDatagram(datagram)
                    }
                }
            } else {
                throw it.cause()
            }
        }
    }
}