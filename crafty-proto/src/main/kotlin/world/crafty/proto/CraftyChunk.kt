package world.crafty.proto

import world.crafty.common.kotlin.computeOnChange
import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.NibbleArray
import world.crafty.common.utils.hashFnv1a64

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
    private var changes = 0
    val hash by computeOnChange({changes}) {
        hashFnv1a64(blocks, data.backing, blockLight.backing, skyLight.backing)
    }

    fun setTypeAndData(x: Int, y: Int, z: Int, type: Int, metadata: Int) {
        val index = getIndex(x, y, z)
        blocks[index] = type.toByte()
        data[index] = metadata
        ++changes
    }

    fun setBlockLight(x: Int, y: Int, z: Int, level: Int) {
        blockLight[getIndex(x, y, z)] = level
        ++changes
    }

    fun setSkyLight(x: Int, y: Int, z: Int, level: Int) {
        skyLight[getIndex(x, y, z)] = level
        ++changes
    }

    private fun getIndex(x: Int, y: Int, z: Int) : Int {
        return ((y % 16) * 256) or (z * 16) or x
    }
    
    init {
        require(blocks.size == blocksPerChunk)
        require(data.backing.size == blocksPerChunk / 2)
        require(blockLight.backing.size == blocksPerChunk / 2)
        require(skyLight.backing.size == blocksPerChunk / 2)
    }
    
    companion object {
        fun createEmpty() : CraftyChunk {
            return CraftyChunk(ByteArray(blocksPerChunk), NibbleArray(blocksPerChunk), NibbleArray(blocksPerChunk), NibbleArray(blocksPerChunk))
        }
        val cachedEmpty = createEmpty()
    }
    
    object Codec : McCodec<CraftyChunk> {
        override fun serialize(obj: CraftyChunk, stream: MinecraftOutputStream) {
            if(obj !is CraftyChunk) throw IllegalArgumentException()
            stream.write(obj.blocks)
            stream.write(obj.data.backing)
            stream.write(obj.blockLight.backing)
            stream.write(obj.skyLight.backing)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyChunk {
            return CraftyChunk(
                    blocks = stream.readByteArray(blocksPerChunk),
                    data = NibbleArray(stream.readByteArray(blocksPerChunk / 2)),
                    blockLight = NibbleArray(stream.readByteArray(blocksPerChunk / 2)),
                    skyLight = NibbleArray(stream.readByteArray(blocksPerChunk / 2))
            )
        }
    }
}