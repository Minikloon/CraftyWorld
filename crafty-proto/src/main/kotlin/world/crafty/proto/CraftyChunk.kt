package world.crafty.proto

import world.crafty.common.utils.LongPackedArray
import world.crafty.common.utils.NibbleArray

private val width = 16
private val height = 16
private val depth = 16
private val blocksPerChunk = width * height * depth

class CraftyChunk(
        val blocks: ByteArray,
        val data: NibbleArray,
        val blockLight: NibbleArray,
        val skyLight: NibbleArray
) {
    val pcTypeAndData: LongPackedArray

    fun setTypeAndData(x: Int, y: Int, z: Int, type: Int, metadata: Int) {
        val index = getIndex(x, y, z)
        blocks[index] = type.toByte()
        data[index] = metadata
        val combined = (type shl 4) or metadata
        pcTypeAndData[index] = combined
    }

    fun setBlockLight(x: Int, y: Int, z: Int, level: Int) {
        blockLight[getIndex(x, y, z)] = level
    }

    fun setSkyLight(x: Int, y: Int, z: Int, level: Int) {
        skyLight[getIndex(x, y, z)] = level
    }

    private fun getIndex(x: Int, y: Int, z: Int) : Int {
        return ((y % 16) * 256) or (z * 16) or x
    }
    
    init {
        require(blocks.size == blocksPerChunk)
        require(data.backing.size == blocksPerChunk / 2)
        require(blockLight.backing.size == blocksPerChunk / 2)
        if(skyLight != null) require(skyLight.backing.size == blocksPerChunk / 2)
        pcTypeAndData = LongPackedArray(13, blocksPerChunk)
        (0 until blocksPerChunk).forEach {
            val combined = ((blocks[it].toInt() and 0xFF) shl 4) or data[it]
            pcTypeAndData[it] = combined
        }
    }
    
    companion object {
        fun createEmpty() : CraftyChunk {
            return CraftyChunk(ByteArray(blocksPerChunk), NibbleArray(blocksPerChunk), NibbleArray(blocksPerChunk), NibbleArray(blocksPerChunk))
        }
        val cachedEmpty = createEmpty()
    }
}