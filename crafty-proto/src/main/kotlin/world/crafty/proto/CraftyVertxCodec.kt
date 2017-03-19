package world.crafty.proto

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import world.crafty.common.serialization.MinecraftInputStream

class CraftyVertxCodec<T: CraftyPacket>(val clazz: Class<T>, val codec: CraftyPacket.CraftyPacketCodec) : MessageCodec<T, T> {
    override fun systemCodecID(): Byte {
        return -1
    }

    override fun name(): String {
        return clazz.name
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): T {
        val size = buffer.getInt(pos)
        val bytes = buffer.getBytes(pos + 4, pos + 4 + size)
        return codec.deserialize(MinecraftInputStream(bytes)) as T
    }

    override fun transform(s: T): T {
        return s
    }

    override fun encodeToWire(buffer: Buffer, s: T) {
        val serialized = s.serialized()
        buffer.appendInt(serialized.size)
        buffer.appendBytes(s.serialized())
    }
}