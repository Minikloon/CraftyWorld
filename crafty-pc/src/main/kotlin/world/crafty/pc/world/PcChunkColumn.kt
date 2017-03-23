package world.crafty.pc.world

import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.compressed
import world.crafty.pc.proto.PrecompressedPayload
import world.crafty.pc.proto.packets.server.ChunkDataPcPacket
import world.crafty.proto.CraftyChunkColumn

class PcChunkColumn(val x: Int, val z: Int, val chunks: Array<PcChunk?>, val biomes: ByteArray, val dimension: Dimension = Dimension.OVERWORLD) {
    constructor(x: Int, z: Int, dimension: Dimension = Dimension.OVERWORLD) : this(
            x,
            z,
            arrayOfNulls(16),
            ByteArray(256),
            dimension
    )
    
    init {
        require(chunks.size == 16)
    }

    fun setTypeAndData(x: Int, y: Int, z: Int, type: Int, data: Int) {
        val section = getSection(y)
        section.setTypeAndData(x, y, z, type, data)
    }

    fun setBiome(x: Int, z: Int, biome: Byte) {
        val index = (z shl 4) or x
        biomes[index] = biome
    }

    fun setBlockLight(x: Int, y: Int, z: Int, level: Int) {
        val section = getSection(y)
        section.setBlockLight(x, y, z, level)
    }

    fun setSkyLight(x: Int, y: Int, z: Int, level: Int) {
        val section = getSection(y)
        section.setBlockLight(x, y, z, level)
    }
    
    private fun getSection(y: Int) : PcChunk {
        val index = y / 16
        var section = chunks[index]
        if(section == null) {
            section = PcChunk(dimension)
            chunks[index] = section
        }
        return section
    }

    private fun getChunkMask() : Int {
        var mask = 0
        for(i in 15 downTo 0) {
            val section = chunks[i]
            mask = mask shl 1
            if(section != null)
                mask = mask or 1
        }
        return mask
    }

    fun toPacket() : ChunkDataPcPacket {
        return ChunkDataPcPacket(x, z, true, getChunkMask(), chunks, biomes)
    }
}

fun CraftyChunkColumn.toPcPacket() : PrecompressedPayload {
    val pcColumn = PcChunkColumn(
            x = x,
            z = z,
            chunks = chunks.map { if(it == null) null else PcChunk.convertCraftyChunk(it) }.toTypedArray(),
            biomes = biomes
    )
    val packet = pcColumn.toPacket()
    val encodedPacket = packet.serializedWithPacketId()
    return PrecompressedPayload(encodedPacket.size, encodedPacket.compressed(CompressionAlgorithm.ZLIB))
}