package world.crafty.pe.world

import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.NibbleArray
import world.crafty.proto.CraftyChunk

private val blocksPerChunk = 16 * 16 * 16

class PeChunk(
        val blocks: ByteArray,
        val data: NibbleArray,
        val skyLight: NibbleArray,
        val blockLight: NibbleArray
) {
    init {
        require(blocks.size == blocksPerChunk)
        require(data.size == blocksPerChunk)
        require(skyLight.size == blocksPerChunk)
        require(blockLight.size == blocksPerChunk)
    }
    
    val byteSize by lazy {
        blocks.size + data.backing.size + skyLight.backing.size + blockLight.backing.size
    }
    
    fun writeToStream(stream: MinecraftOutputStream) {
        stream.writeByte(0) // chunk mode
        stream.write(blocks)
        stream.write(data.backing)
        stream.write(skyLight.backing)
        stream.write(blockLight.backing)
    }
    
    companion object {
        fun convertFromCrafty(craftyChunk: CraftyChunk) : PeChunk {
            return PeChunk(
                    blocks = yxzOrderToXzy(craftyChunk.blocks),
                    data = yxzOrderToXzy(craftyChunk.data),
                    skyLight = yxzOrderToXzy(craftyChunk.skyLight),
                    blockLight = yxzOrderToXzy(craftyChunk.blockLight)
            )
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
        
        val cachedEmpty = convertFromCrafty(CraftyChunk.cachedEmpty)!!
    }
}