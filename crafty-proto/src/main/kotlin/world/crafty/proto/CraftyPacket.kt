package world.crafty.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

abstract class CraftyPacket {
    abstract val codec: CraftyPacketCodec
    
    fun serialize(stream: OutputStream) {
        val mcStream = MinecraftOutputStream(stream)
        codec.serialize(this, mcStream)
    }
    
    fun serialized() : ByteArray {
        val bs = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(bs)
        serialize(mcStream)
        return bs.toByteArray()
    }
    
    abstract class CraftyPacketCodec {
        abstract fun serialize(obj: Any, stream: MinecraftOutputStream)
        abstract fun deserialize(stream: MinecraftInputStream) : CraftyPacket
    }
}