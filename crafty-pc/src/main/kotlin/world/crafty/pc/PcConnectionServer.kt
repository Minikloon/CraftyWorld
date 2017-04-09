package world.crafty.pc

import io.vertx.core.AbstractVerticle
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.getLogger
import world.crafty.common.utils.info
import world.crafty.mojang.MojangClient
import world.crafty.pc.metadata.translators.MetaTranslatorRegistry
import world.crafty.pc.metadata.translators.registerBuiltInPcTranslators
import world.crafty.pc.proto.packets.server.ServerKeepAlivePcPacket
import world.crafty.pc.proto.PrecompressedPayload
import world.crafty.proto.ConcurrentColumnsCache
import world.crafty.proto.metadata.MetaFieldRegistry
import world.crafty.proto.metadata.registerBuiltInMetaDefinitions
import world.crafty.proto.registerVertxCraftyCodecs
import world.crafty.skinpool.protocol.registerVertxSkinPoolCodecs
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher

private val log = getLogger<PcConnectionServer>()
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
        server.connectHandler {
            val session = PcNetworkSession(this, worldServer, it)
            sessions[it] = session
            it.handler { session.receive(it) }
            log.info { "Received PC connection from ${it.remoteAddress()}" }
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
    
    fun getWorldCache(worldName: String) : ConcurrentColumnsCache<PrecompressedPayload> {
        return worldCaches.getOrPut(worldName) { ConcurrentColumnsCache() }
    }
}