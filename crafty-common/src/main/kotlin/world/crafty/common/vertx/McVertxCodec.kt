package world.crafty.common.vertx

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageCodec
import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

class McVertxCodec<T>(private val clazz: Class<T>, private val codec: McCodec<T>) : MessageCodec<T, T> {
    override fun encodeToWire(buffer: Buffer, s: T) {
        val bs = BufferOutputStream(buffer)
        val mc = MinecraftOutputStream(bs)
        codec.serialize(s, mc)
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): T {
        val bs = BufferInputStream(buffer, pos)
        val mc = MinecraftInputStream(bs)
        return codec.deserialize(mc)
    }
    
    override fun systemCodecID(): Byte {
        return -1
    }

    override fun name(): String {
        return clazz.name
    }

    override fun transform(s: T): T {
        return s
    }
}

inline fun <reified T> EventBus.registerDefaultCodec(codec: McCodec<T>) {
    val clazz = T::class.java
    registerDefaultCodec(clazz, McVertxCodec(clazz, codec))
}