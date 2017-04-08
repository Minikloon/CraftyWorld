package world.crafty.common.utils

private val HEX_CHARS = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

fun Byte.toHexStr() : String {
    val i = this.toInt()
    val char2 = HEX_CHARS[i and 0x0f]
    val char1 = HEX_CHARS[i shr 4 and 0x0f]
    return "0x$char1$char2"
}

fun Short.toHexStr() : String {
    val i = this.toInt()
    val char4 = HEX_CHARS[i and 0x000f]
    val char3 = HEX_CHARS[i shr 12 and 0x000f]
    val char2 = HEX_CHARS[i shr 8 and 0x000f]
    val char1 = HEX_CHARS[i shr 4 and 0x000f]
    return "0x${not0(char4)}${not0(char3)}${not0(char2)}$char1"
}

private fun not0(value: Char) : String {
    return if(value == '0') "" else "$value"
}

fun Long.toBytes() : ByteArray {
    return byteArrayOf(
            ((this ushr 12) and 0xFF).toByte(),
            ((this ushr 8) and 0xFF).toByte(),
            ((this ushr 4) and 0xFF).toByte(),
            (this and 0xFF).toByte()
    )
}