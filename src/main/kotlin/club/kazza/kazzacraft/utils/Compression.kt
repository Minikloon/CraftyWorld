package club.kazza.kazzacraft.utils

import java.util.zip.Deflater
import java.util.zip.Inflater

private val inflaterLocal = ThreadLocal.withInitial { Inflater() }
private val deflaterLocal = ThreadLocal.withInitial { Deflater() }

fun ByteArray.decompress(algo: CompressionAlgorithm, expectedSize: Int) : ByteArray {    
    val inflater = inflaterLocal.get()
    inflater.setInput(this)
    val output = ByteArray(expectedSize)
    inflater.inflate(output)
    return output
}

fun ByteArray.compress(algo: CompressionAlgorithm) : ByteArray {
    val deflater = deflaterLocal.get()
    deflater.setInput(this)
    val output = ByteArray(this.size)
    val compressedSize = deflater.deflate(output)
    return output.sliceArray(0 until compressedSize)
}

enum class CompressionAlgorithm {
    ZLIB
}