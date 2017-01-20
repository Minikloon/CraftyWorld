package club.kazza.kazzacraft.utils

private val HEX_CHARS = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

fun Byte.toHexStr() : String {
    val i = this.toInt()
    val char2 = HEX_CHARS[i and 0x0f]
    val char1 = HEX_CHARS[i shr 4 and 0x0f]
    return "0x$char1$char2"
}

fun Int.toHexStr() : String {
    val i = this
    val char4 = HEX_CHARS[i and 0x000f]
    val char3 = HEX_CHARS[i shr 12 and 0x000f]
    val char2 = HEX_CHARS[i shr 8 and 0x000f]
    val char1 = HEX_CHARS[i shr 4 and 0x000f]
    return "0x$char4$char3$char2$char1"
}