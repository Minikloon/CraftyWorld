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

class ChunkSection(dimension: Dimension) {
    private val typeAndData = LongPackedArray(bitsPerBlock, blocksPerSection)
    private val blockLight = NibbleArray(blocksPerSection)
    private val skyLight: NibbleArray? = if(dimension == Dimension.OVERWORLD) NibbleArray(blocksPerSection) else null
    val byteSize = 2 + varIntSize(832) + typeAndData.backing.size * 8 + blockLight.backing.size + if(skyLight == null) 0 else skyLight.backing.size

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
        return ((y and 0xf) shl 8) or (z shl 4) or x
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