package club.kazza.kazzacraft.network

import io.vertx.core.AbstractVerticle
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.impl.ContextImpl
import io.vertx.core.net.SocketAddress
import java.util.concurrent.CompletableFuture

class PeConnectionServer(val port: Int) : AbstractVerticle() {
    lateinit var socket: DatagramSocket
    val sessions: MutableMap<SocketAddress, CompletableFuture<PeNetworkSession>> = mutableMapOf()

    override fun start() {
        socket = vertx.createDatagramSocket()
        socket.listen(port, "0.0.0.0") {
            if(it.succeeded()) {
                socket.handler {
                    val data = it.data()
                    val sender = it.sender()
                    if(data == null || sender == null)
                        return@handler
                    val getSession = sessions.getOrPut(sender) {
                        val future = CompletableFuture<PeNetworkSession>()
                        val session = PeNetworkSession(socket, sender)
                        vertx.deployVerticle(session) {
                            future.complete(session)
                        }
                        future
                    }
                    getSession.thenAcceptAsync {
                        val session = it
                        session.queueReceive(data)
                    }
                }
            } else {
                throw it.cause()
            }
        }
    }
}