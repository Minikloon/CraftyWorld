package club.kazza.kazzacraft.world

import club.kazza.kazzacraft.network.protocol.Pc

class ChunkColumn(val x: Int, val z: Int, val dimension: Dimension) {
    private val sections: Array<ChunkSection?> = arrayOfNulls(16)
    private val biomes = ByteArray(256)

    fun setTypeAndData(x: Int, y: Int, z: Int, type: Int, data: Int) {
        val section = getSection(y)
        section.setTypeAndData(x, y, z, type, data)
    }

    fun setBiome(x: Int, z: Int) {
        val index = (z shl 4) or x
    }

    private fun getChunkMask() : Int {
        var mask = 0
        sections.forEach {
            mask = mask shl 1
            if(it != null)
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

    fun toPacket() : Pc.Server.Play.ChunkDataPcPacket {
        val chunkMask = getChunkMask()
        val continuous = chunkMask == 0xFFFF
        return Pc.Server.Play.ChunkDataPcPacket(x, z, continuous, chunkMask, sections, if(continuous) biomes else null)
    }
}