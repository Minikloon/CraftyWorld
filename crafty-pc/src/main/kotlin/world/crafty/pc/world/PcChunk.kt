package world.crafty.pc.world

import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.serialization.MinecraftOutputStream.Companion.varIntSize
import world.crafty.common.utils.LongPackedArray
import world.crafty.common.utils.NibbleArray
import world.crafty.proto.CraftyChunk

private val blocksPerChunk = 16 * 16 * 16
private val bitsPerBlock = 13

class PcChunk(
        val typeAndData: LongPackedArray, 
        val blockLight: NibbleArray, 
        val skyLight: NibbleArray?
) {
    init {
        require(typeAndData.backing.size == LongPackedArray.requiredLongs(bitsPerBlock, blocksPerChunk))
        require(blockLight.size == blocksPerChunk)
        if(skyLight != null) require(skyLight.size == blocksPerChunk)
    }
    
    val byteSize by lazy {
        2 + varIntSize(typeAndData.backing.size * 8) + typeAndData.backing.size * 8 + blockLight.backing.size + (skyLight?.backing?.size ?: 0)
    }

    constructor(dimension: Dimension = Dimension.OVERWORLD) : this(
            NibbleArray(blocksPerChunk),
            if(dimension == Dimension.OVERWORLD) NibbleArray(blocksPerChunk) else null
    )

    constructor(blockLight: NibbleArray, skyLight: NibbleArray?) : this(
            LongPackedArray(bitsPerBlock, blocksPerChunk),
            blockLight,
            skyLight
    )
    
    constructor(type: ByteArray, data: NibbleArray, blockLight: NibbleArray, skyLight: NibbleArray?) : this(
            typesAndDataLongPacked(type, data),
            blockLight,
            skyLight
    )

    fun setTypeAndData(x: Int, y: Int, z: Int, type: Int, data: Int) {
        val combined = (type shl 4) or data
        typeAndData[getIndex(x, y, z)] = combined
    }

    fun setBlockLight(x: Int, y: Int, z: Int, level: Int) {
        blockLight[getIndex(x, y, z)] = level
    }

    fun setSkyLight(x: Int, y: Int, z: Int, level: Int) {
        if(skyLight == null) return
        skyLight[getIndex(x, y, z)] = level
    }

    private fun getIndex(x: Int, y: Int, z: Int) : Int {
        return ((y % 16) shl 8) or (z shl 4) or x
    }

    fun writeToStream(stream: MinecraftOutputStream) {
        stream.writeByte(bitsPerBlock)
        stream.writeSignedVarInt(0)
        stream.writeSignedVarInt(typeAndData.backing.size)
        typeAndData.backing.forEach { stream.writeLong(it) }
        stream.write(blockLight.backing)
        if(skyLight != null)
            stream.write(skyLight.backing)
    }

    companion object {
        fun typesAndDataLongPacked(types: ByteArray, data: NibbleArray) : LongPackedArray {
            val packed = LongPackedArray(bitsPerBlock, blocksPerChunk)
            for(i in 0 until blocksPerChunk) {
                val combined = ((types[i].toInt() and 0xFF) shl 4) or data[i]
                packed[i] = combined
            }
            return packed
        }
        
        fun convertCraftyChunk(chunk: CraftyChunk) : PcChunk {
            return PcChunk(
                    typeAndData = typesAndDataLongPacked(chunk.blocks, chunk.data),
                    blockLight = chunk.blockLight,
                    skyLight = chunk.skyLight
            )
        }
    }
}