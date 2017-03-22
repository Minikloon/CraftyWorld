package world.crafty.pc.world

import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.serialization.MinecraftOutputStream.Companion.varIntSize
import world.crafty.common.utils.LongPackedArray
import world.crafty.common.utils.NibbleArray

private val width = 16
private val height = 16
private val depth = 16
private val blocksPerSection = width * height * depth
private val bitsPerBlock = 13

class PcChunk(val typeAndData: LongPackedArray, val blockLight: NibbleArray, val skyLight: NibbleArray?) {
    val byteSize by lazy {
        2 + varIntSize(typeAndData.backing.size * 8) + typeAndData.backing.size * 8 + blockLight.backing.size + (skyLight?.backing?.size ?: 0)
    }

    constructor(dimension: Dimension = Dimension.OVERWORLD) : this(
            NibbleArray(blocksPerSection),
            if(dimension == Dimension.OVERWORLD) NibbleArray(blocksPerSection) else null
    )

    constructor(blockLight: NibbleArray, skyLight: NibbleArray?) : this(
            LongPackedArray(bitsPerBlock, blocksPerSection),
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
}