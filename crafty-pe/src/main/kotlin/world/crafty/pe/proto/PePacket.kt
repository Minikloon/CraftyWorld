package world.crafty.pe.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

abstract class PePacket {
    abstract val id: Int
    abstract val codec: PePacketCodec

    open val expectedSize = 32
    
    fun serialize(stream: OutputStream) {
        val mcStream = MinecraftOutputStream(stream)
        codec.serialize(this, mcStream)
    }

    fun serialized() : ByteArray {
        return MinecraftOutputStream.serialized(expectedSize) {
            serialize(it)
        }
    }

    fun serializedWithId() : ByteArray {
        return MinecraftOutputStream.serialized(expectedSize) { stream ->
            stream.writeByte(id)
            serialize(stream)
        }
    }

    abstract class PePacketCodec {
        abstract val id: Int
        abstract fun serialize(obj: Any, stream: MinecraftOutputStream)
        fun deserialize(stream: InputStream) : PePacket { return deserialize(MinecraftInputStream(stream)); }
        abstract fun deserialize(stream: MinecraftInputStream) : PePacket
    }
}