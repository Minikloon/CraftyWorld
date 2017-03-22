package world.crafty.pc.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.compressed
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

abstract class PcPacket : LengthPrefixedContent {
    abstract val id: Int
    abstract val codec: PcPacketCodec
    
    private fun serializedWithPacketId() : ByteArray {
        val bs = ByteArrayOutputStream()
        val stream = MinecraftOutputStream(bs)
        stream.writeSignedVarInt(id)
        codec.serialize(this, stream)
        return bs.toByteArray()
    }
    
    override fun serializeWithLengthPrefix(stream: MinecraftOutputStream, compressing: Boolean, compressionThreshold: Int) {
        val content = serializedWithPacketId()
        if(compressing) {
            if(content.size >= compressionThreshold) {
                val compressed = content.compressed(CompressionAlgorithm.ZLIB)
                stream.writeSignedVarInt(MinecraftOutputStream.varIntSize(content.size) + compressed.size)
                stream.writeSignedVarInt(content.size)
                stream.write(compressed)
            }
            else {
                stream.writeSignedVarInt(1 + content.size)
                stream.writeSignedVarInt(0)
                stream.write(content)
            }
        } else {
            stream.writeSignedVarInt(content.size)
            stream.write(content)
        }
    }

    abstract class PcPacketCodec {
        abstract val id: Int
        open val expectedSize: Int = 24
        abstract fun serialize(obj: Any, stream: MinecraftOutputStream)
        abstract fun deserialize(stream: MinecraftInputStream) : PcPacket
    }
}