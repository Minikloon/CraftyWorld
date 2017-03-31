package world.crafty.proto

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.vertx.BufferInputStream
import world.crafty.common.vertx.BufferOutputStream

class CraftyVertxCodec<T: CraftyPacket>(val clazz: Class<T>, val codec: CraftyPacket.CraftyPacketCodec) : MessageCodec<T, T> {
    override fun encodeToWire(buffer: Buffer, s: T) {
        val bs = BufferOutputStream(buffer)
        val mc = MinecraftOutputStream(bs)
        codec.serialize(s, mc)
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): T {
        val bs = BufferInputStream(buffer)
        val mc = MinecraftInputStream(bs)
        return codec.deserialize(mc) as T
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