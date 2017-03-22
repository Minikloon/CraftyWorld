package world.crafty.server.world.serialize

import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.NibbleArray
import world.crafty.common.utils.compressed
import world.crafty.proto.CraftyChunk
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.server.SetChunkColumnCraftyPacket
import java.io.ByteArrayOutputStream

object PeFullChunkPacketEncoder : FullChunkPacketEncoder {
    val compressionWrapperPacketId = 0x06
    val chunkDataPacketId = 0x3a
    override fun toPacket(obj: CraftyChunkColumn) : SetChunkColumnCraftyPacket {
        val encodedColumn = serializedChunkColumn(obj)
        val fullChunkPacket = serializedFullChunkPacket(obj, encodedColumn)
        
        val bs = ByteArrayOutputStream()
        writeCompressionWrapperToStream(fullChunkPacket, MinecraftOutputStream(bs))
        return SetChunkColumnCraftyPacket(MinecraftPlatform.PE, fullChunkPacket.size, bs.toByteArray())
    }
    private fun serializedChunkColumn(column: CraftyChunkColumn) : ByteArray {
        val columnBs = ByteArrayOutputStream()
        val stream = MinecraftOutputStream(columnBs)

        val highestNonAirChunkIndex = column.chunks.indexOfLast { !(it?.blocks?.all { it == 0.toByte() } ?: true) }
        stream.writeByte(highestNonAirChunkIndex + 1) // number of chunks sent
        (0..highestNonAirChunkIndex)
                .map { column.chunks[it] }
                .forEach {
                    if(it == null)
                        writeChunkToStream(CraftyChunk.cachedEmpty, stream)
                    else
                        writeChunkToStream(it, stream)
                }
        stream.write(ByteArray(256*2)) // height map
        stream.write(ByteArray(16*16)) // biome ids
        stream.writeByte(0) // something about border blocks

        stream.writeZigzagVarInt(0) // blocks entities

        return columnBs.toByteArray()
    }
    private fun writeChunkToStream(chunk: CraftyChunk, stream: MinecraftOutputStream) {
        stream.writeByte(0) // chunk mode
        stream.write(yxzOrderToXzy(chunk.blocks))
        stream.write(yxzOrderToXzy(chunk.data).backing)
        stream.write(yxzOrderToXzy(chunk.skyLight).backing)
        stream.write(yxzOrderToXzy(chunk.blockLight).backing)
    }
    private fun serializedFullChunkPacket(column: CraftyChunkColumn, chunkData: ByteArray) : ByteArray {
        val bs = ByteArrayOutputStream()
        val stream = MinecraftOutputStream(bs)
        stream.writeZigzagVarInt(column.x)
        stream.writeZigzagVarInt(column.z)
        stream.writeUnsignedVarInt(chunkData.size)
        stream.write(chunkData)
        return bs.toByteArray()
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