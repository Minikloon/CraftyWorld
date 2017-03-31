package world.crafty.skinpool.protocol

import io.vertx.core.eventbus.EventBus
import world.crafty.common.vertx.registerDefaultCodec
import world.crafty.skinpool.protocol.client.*
import world.crafty.skinpool.protocol.server.*

private var registered = false
private val lock = Any()

fun registerVertxSkinPoolCodecs(eb: EventBus) {
    synchronized(lock) {
        if(registered) return
        registered = true
    }
    
    eb.registerDefaultCodec(HashPollPoolPacket.Codec)
    eb.registerDefaultCodec(HashPollReplyPoolPacket.Codec)
    eb.registerDefaultCodec(SaveSkinPoolPacket.Codec)
    eb.registerDefaultCodec(SaveProfilePoolPacket.Codec)
}