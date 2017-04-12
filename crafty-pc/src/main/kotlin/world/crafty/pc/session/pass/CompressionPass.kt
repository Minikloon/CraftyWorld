package world.crafty.pc.session.pass

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.decompressed

interface CompressionPass {
    fun decompressedStream(stream: MinecraftInputStream) : MinecraftInputStream
    
    val compressing: Boolean
    val threshold: Int
}

object NoCompressionPass : CompressionPass {
    override fun decompressedStream(stream: MinecraftInputStream) : MinecraftInputStream {
        return stream
    }

    override val compressing = false
    override val threshold = 0
}

class MinecraftCompressionPass : CompressionPass {
    override fun decompressedStream(stream: MinecraftInputStream): MinecraftInputStream {
        val decompressedLength = stream.readSignedVarInt()
        if(decompressedLength < threshold)
            return stream
        val decompressed = stream.readRemainingBytes().decompressed(CompressionAlgorithm.ZLIB, decompressedLength)
        return MinecraftInputStream(decompressed)
    }
    
    override val compressing = true
    override val threshold = 256
}