package club.kazza.kazzacraft.world

import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream.Companion.varIntSize
import club.kazza.kazzacraft.utils.LongPackedArray
import club.kazza.kazzacraft.utils.NibbleArray
import java.util.*

private val width = 16
private val height = 16
private val depth = 16
private val blocksPerSection = width * height * depth
private val bitsPerBlock = 13

class ChunkSection(val typeAndData: LongPackedArray, val blockLight: NibbleArray, val skyLight: NibbleArray?) {
    val byteSize = 2 + varIntSize(typeAndData.backing.size * 8) + typeAndData.backing.size * 8 + blockLight.backing.size + if(skyLight == null) 0 else skyLight.backing.size

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
        stream.writeVarInt(0)
        stream.writeVarInt(typeAndData.backing.size)
        typeAndData.backing.forEach { stream.writeLong(it) }
        stream.write(blockLight.backing)
        if(skyLight != null)
            stream.write(skyLight.backing)
    }
}