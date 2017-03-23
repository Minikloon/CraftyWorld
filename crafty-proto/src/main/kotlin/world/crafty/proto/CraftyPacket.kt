package world.crafty.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

abstract class CraftyPacket {
    abstract val codec: CraftyPacketCodec
    open val expectedSize = 32
    
    fun serialize(stream: OutputStream) {
        val mcStream = MinecraftOutputStream(stream)
        codec.serialize(this, mcStream)
    }
    
    fun serialized() : ByteArray {
        return MinecraftOutputStream.serialized(expectedSize) { stream ->
            serialize(stream)
        }
    }
    
    abstract class CraftyPacketCodec {
        abstract fun serialize(obj: Any, stream: MinecraftOutputStream)
        abstract fun deserialize(stream: MinecraftInputStream) : CraftyPacket
    }
}