package world.crafty.server.world.serialize

import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.compressed
import world.crafty.proto.CraftyChunk
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.server.SetChunkColumnCraftyPacket
import java.io.ByteArrayOutputStream

object PcFullChunkPacketEncoder : FullChunkPacketEncoder {
    private val packetId = 0x20
    private val bitsPerBlock = 13
    override fun toPacket(chunk: CraftyChunkColumn) : SetChunkColumnCraftyPacket {
        val encoded = serializedFullChunkPacket(chunk)
        val compressed = encoded.compressed(CompressionAlgorithm.ZLIB)
        return SetChunkColumnCraftyPacket(MinecraftPlatform.PC, encoded.size, compressed)
    }
    private fun serializedFullChunkPacket(chunk: CraftyChunkColumn) : ByteArray {
        val bs = ByteArrayOutputStream()
        val stream = MinecraftOutputStream(bs)
        stream.writeSignedVarInt(packetId)

        stream.writeInt(chunk.x)
        stream.writeInt(chunk.z)
        stream.writeBoolean(true)
        stream.writeSignedVarInt(getChunkMask(chunk))

        val chunksBs = ByteArrayOutputStream()
        val chunksStream = MinecraftOutputStream(chunksBs)
        chunk.chunks.filterNotNull().forEach { writeChunkToStream(it, chunksStream) }
        val chunkBytes = chunksBs.toByteArray()
        stream.writeSignedVarInt(chunkBytes.size + chunk.biomes.size)
        stream.write(chunkBytes)
        stream.write(chunk.biomes)

        stream.writeSignedVarInt(0) // blocks entities
        return bs.toByteArray()
    }
    private fun getChunkMask(column: CraftyChunkColumn) : Int {
        var mask = 0
        for(i in 15 downTo 0) {
            val chunk = column.chunks[i]
            mask = mask shl 1
            if(chunk != null)
                mask = mask or 1
        }
        return mask
    }
    private fun writeChunkToStream(chunk: CraftyChunk, stream: MinecraftOutputStream) {
        stream.writeByte(bitsPerBlock)
        stream.writeSignedVarInt(0)
        stream.writeSignedVarInt(chunk.pcTypeAndData.backing.size)
        chunk.pcTypeAndData.backing.forEach { stream.writeLong(it) }
        stream.write(chunk.blockLight.backing)
        stream.write(chunk.skyLight.backing)
    }
}