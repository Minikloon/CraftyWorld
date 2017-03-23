package world.crafty.pe.proto

import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.NibbleArray
import world.crafty.common.utils.compressed
import world.crafty.pe.proto.packets.mixed.CompressionWrapperPePacket
import world.crafty.pe.proto.packets.mixed.EncryptionWrapperPePacket
import world.crafty.pe.proto.packets.server.FullChunkDataPePacket
import world.crafty.pe.world.PeChunk
import world.crafty.pe.world.PeChunkColumn
import world.crafty.proto.CraftyChunkColumn
import java.io.ByteArrayOutputStream

object PeFullChunkPacketEncoder {
    val compressionWrapperPacketId = 0x06
    val chunkDataPacketId = 0x3a

    fun toPacket(obj: CraftyChunkColumn) : EncryptionWrapperPePacket {
        val peColumn = PeChunkColumn(
                x = obj.x,
                z = obj.z,
                chunks = obj.chunks.map { if(it == null) null else PeChunk.convertFromCrafty(it) }.toTypedArray(),
                biomes = obj.biomes,
                heightMap = ByteArray(16 * 16 * 2) // TODO: compute this?
        )
        val chunkPacket = FullChunkDataPePacket(peColumn)

        val compressed = CompressionWrapperPePacket(chunkPacket)
        return EncryptionWrapperPePacket(compressed.serializedWithId())
    }
    
    private fun writeCompressionWrapperToStream(fullChunkData: ByteArray, stream: MinecraftOutputStream) {
        val compressedBs = ByteArrayOutputStream()
        val compressedStream = MinecraftOutputStream(compressedBs)
        compressedStream.writeUnsignedVarInt(fullChunkData.size + 1) // 1 for packet id
        compressedStream.writeByte(chunkDataPacketId)
        compressedStream.write(fullChunkData)

        val compressed = compressedBs.toByteArray().compressed(CompressionAlgorithm.ZLIB)

        stream.writeByte(compressionWrapperPacketId)
        stream.writeUnsignedVarInt(compressed.size)
        stream.write(compressed)
    }
    private fun yxzOrderToXzy(bytes: ByteArray) : ByteArray {
        val reordered = ByteArray(bytes.size)
        var index = 0
        for(x in 0 until 16) {
            for(z in 0 until 16) {
                for(y in 0 until 16) {
                    val originalIndex = (y * 256) or (z * 16) or x
                    reordered[index] = bytes[originalIndex]
                    index++
                }
            }
        }
        return reordered
    }
    private fun yxzOrderToXzy(nibbles: NibbleArray) : NibbleArray {
        val reordered = NibbleArray(nibbles.size)
        var index = 0
        for(x in 0 until 16) {
            for(z in 0 until 16) {
                for(y in 0 until 16) {
                    val originalIndex = (y * 256) or (z * 16) or x
                    reordered[index] = nibbles[originalIndex]
                    index++
                }
            }
        }
        return reordered
    }
}