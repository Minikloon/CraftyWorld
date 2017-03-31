package world.crafty.pe

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun isSlim(rawBytes: ByteArray) : Boolean {
    return rawBytes.size == 64 * 64 * 4
}

fun peSkinToPng(rawBytes: ByteArray) : ByteArray {
    val slim = isSlim(rawBytes)
    val width = 64
    val height = if (slim) 64 else 32

    val img = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
    img.setRGB(0, 0, width, height, rgbaToArgbInt(rawBytes), 0, width)

    val bs = ByteArrayOutputStream(64*64)
    ImageIO.write(img, "png", bs)
    return bs.toByteArray()
}

private fun rgbaToArgbInt(bytes: ByteArray) : IntArray {
    val rgba = bytes.map { it.toInt() and 0xFF }
    return IntArray(bytes.size / 4) { index ->
        val i = index * 4
        (rgba[i+3] shl 24) or (rgba[i+0] shl 16) or (rgba[i+1] shl 8) or rgba[i+2]
    }
}

fun pngToRawSkin(pngBytes: ByteArray) : ByteArray {
    val img = ImageIO.read(ByteArrayInputStream(pngBytes))
    val ints = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
    return argbIntToRgba(ints)
}

private fun argbIntToRgba(ints: IntArray) : ByteArray {
    return ByteArray(ints.size * 4) {
        val int = ints[it / 4]
        val mod = it % 4
        val shiftBits = 
                if(mod == 3) 24 
                else (2 - mod) * 8
        
        ((int shl shiftBits) and 0xFF).toByte()
    }
}