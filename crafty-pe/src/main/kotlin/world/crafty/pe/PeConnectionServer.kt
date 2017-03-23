package world.crafty.pe

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.AbstractVerticle
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.json.Json
import io.vertx.core.net.SocketAddress
import world.crafty.pe.proto.packets.mixed.EncryptionWrapperPePacket
import world.crafty.proto.ConcurrentColumnsCache
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class PeConnectionServer(val port: Int, val worldServer: String) : AbstractVerticle() {
    lateinit var socket: DatagramSocket
    val sessions: MutableMap<SocketAddress, CompletableFuture<PeNetworkSession>> = mutableMapOf()
    val supportsEncryption = false
    private val worldCaches = ConcurrentHashMap<String, ConcurrentColumnsCache<EncryptionWrapperPePacket>>() // TODO: share between connections server somehow
    
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
                        val session = PeNetworkSession(this, worldServer, socket, sender)
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
    
    fun getWorldCache(worldName: String) : ConcurrentColumnsCache<EncryptionWrapperPePacket> {
        return worldCaches.getOrPut(worldName) { ConcurrentColumnsCache() }
    }
}