package world.crafty.proto

class CraftyChunkColumn(
        val x: Int,
        val z: Int,
        val chunks: Array<CraftyChunk?>,
        val biomes: ByteArray
) {
    constructor(x: Int, z: Int) : this(
            x, z,
            arrayOfNulls(16),
            ByteArray(256)
    )
    
    init {
        require(biomes.size == 256)
    }

    fun setTypeAndData(x: Int, y: Int, z: Int, type: Int, data: Int) {
        val chunk = getChunk(y)
        chunk.setTypeAndData(x, y, z, type, data)
    }

    fun setBlockLight(x: Int, y: Int, z: Int, level: Int) {
        val chunk = getChunk(y)
        chunk.setBlockLight(x, y, z, level)
    }

    fun setSkyLight(x: Int, y: Int, z: Int, level: Int) {
        val chunk = getChunk(y)
        chunk.setBlockLight(x, y, z, level)
    }

    fun setBiome(x: Int, z: Int, biome: Byte) {
        val index = (z shl 4) or x
        biomes[index] = biome
    }

    private fun getChunk(y: Int) : CraftyChunk {
        val index = y / 16
        var chunk = chunks[index]
        if(chunk == null) {
            chunk = CraftyChunk.createEmpty()
            chunks[index] = chunk
        }
        return chunk
    }
}