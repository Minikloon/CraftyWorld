package world.crafty.pc.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

abstract class PcPacket {
    abstract val id: Int
    abstract val codec: PcPacketCodec

    fun serialized() : ByteArray {
        val bs = ByteArrayOutputStream()
        val stream = MinecraftOutputStream(bs)
        codec.serialize(this, stream)
        return bs.toByteArray()
    }
    
    fun serializeNoHeader(stream: MinecraftOutputStream) {
        codec.serialize(this, stream)
    }
    
    fun serializeWithHeader(stream: MinecraftOutputStream, compressed: Boolean = false) {
        val content = serialized()
        stream.writeUnsignedVarInt(content.size + 1 + (if(compressed) 1 else 0))
        if(compressed) stream.writeUnsignedVarInt(0)
        stream.writeUnsignedVarInt(id)
        stream.write(content)
    }

    abstract class PcPacketCodec {
        abstract val id: Int
        open val expectedSize: Int = 24
        abstract fun serialize(obj: Any, stream: MinecraftOutputStream)
        fun deserialize(stream: InputStream) : PcPacket { return deserialize(MinecraftInputStream(stream)); }
        abstract fun deserialize(stream: MinecraftInputStream) : PcPacket
    }
}