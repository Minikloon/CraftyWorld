package world.crafty.pc

import io.vertx.core.AbstractVerticle
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.logger
import world.crafty.mojang.MojangClient
import world.crafty.pc.metadata.translators.MetaTranslatorRegistry
import world.crafty.pc.metadata.translators.registerBuiltInPcTranslators
import world.crafty.pc.proto.LengthPrefixedHandler
import world.crafty.pc.proto.packets.server.ServerKeepAlivePcPacket
import world.crafty.pc.proto.PrecompressedPayload
import world.crafty.proto.ConcurrentColumnsCache
import world.crafty.proto.metadata.MetaFieldRegistry
import world.crafty.proto.metadata.registerBuiltInMetaDefinitions
import world.crafty.proto.registerVertxCraftyCodecs
import world.crafty.skinpool.protocol.registerVertxSkinPoolCodecs
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher

private val log = logger<PcConnectionServer>()
class PcConnectionServer(val port: Int, val worldServer: String) : AbstractVerticle() {
    lateinit var server: NetServer
    private val sessions = mutableMapOf<NetSocket, PcNetworkSession>()
    private val worldCaches = ConcurrentHashMap<String, ConcurrentColumnsCache<PrecompressedPayload>>() // TODO: share between connections server somehow
    val metaTranslatorRegistry = MetaTranslatorRegistry()

    lateinit var mojang: MojangClient

    val decipher: Cipher
    val x509PubKey: ByteArray
    val encodedBrand: ByteArray

    init {
        val rsaKey = generateKeyPair()
        x509PubKey = convertKeyToX509(rsaKey.public).encoded
        decipher = createDecipher(rsaKey)
        
        encodedBrand = MinecraftOutputStream.serialized {
            it.writeSignedString("crafty")
        }
        
        metaTranslatorRegistry.registerBuiltInPcTranslators()
    }

    override fun start() {
        val eb = vertx.eventBus()
        registerVertxCraftyCodecs(eb)
        registerVertxSkinPoolCodecs(eb)
        MetaFieldRegistry.registerBuiltInMetaDefinitions()
        
        mojang = MojangClient(vertx)

        server = vertx.createNetServer()
        server.connectHandler { socket ->
            log.info { "PC connection from ${socket.remoteAddress()}" }
            val session = PcNetworkSession(this, worldServer, socket)
            sessions[socket] = session
            socket.handler {
                session.receive(it)
            }
            socket.endHandler {
                session.disconnect("Disconnected")
            }
        }
        server.listen(port)
    }
    
    fun removeSessionSocket(address: NetSocket) {
        sessions.remove(address)
    }
    
    fun getWorldCache(worldName: String) : ConcurrentColumnsCache<PrecompressedPayload> {
        return worldCaches.getOrPut(worldName) { ConcurrentColumnsCache() }
    }
}