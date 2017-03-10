package world.crafty.pc.world

import world.crafty.pc.world.ChunkSection
import world.crafty.pc.world.Dimension
import world.crafty.pc.proto.packets.server.ChunkDataPcPacket

class ChunkColumn(val x: Int, val z: Int, val sections: Array<ChunkSection?>, val biomes: ByteArray, val dimension: Dimension = Dimension.OVERWORLD) {
    constructor(x: Int, z: Int, dimension: Dimension = Dimension.OVERWORLD) : this(
            x,
            z,
            arrayOfNulls(16),
            ByteArray(256),
            dimension
    )

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

    private fun getChunkMask() : Int {
        var mask = 0
        for(i in 15 downTo 0) {
            val section = sections[i]
            mask = mask shl 1
            if(section != null)
                mask = mask or 1
        }
        return mask
    }

    private fun getSection(y: Int) : ChunkSection {
        val index = y / 16
        var section = sections[index]
        if(section == null) {
            section = ChunkSection(dimension)
            sections[index] = section
        }
        return section
    }

    fun toPacket() : ChunkDataPcPacket {
        val chunkMask = getChunkMask()
        return ChunkDataPcPacket(x, z, true, chunkMask, sections, biomes)
    }
}