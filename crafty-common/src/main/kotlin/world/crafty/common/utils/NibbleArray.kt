package world.crafty.common.utils

class NibbleArray(val backing: ByteArray) {
    constructor(size: Int) : this(ByteArray(size/2))

    operator fun get(index: Int) : Int {
        val value = backing[index / 2].toInt()
        return if(index % 2 == 0) (value and 0x0f) else ((value and 0xf0) shr 4)
    }

    operator fun set(index: Int, value: Int) {
        val nibble = value and 0xf
        val halfIndex = index / 2
        val previous = backing[halfIndex].toInt()
        if(index % 2 == 0) {
            backing[halfIndex] = ((previous and 0xf0) or nibble).toByte()
        } else {
            backing[halfIndex] = ((previous and 0x0f) or (nibble shl 4)).toByte()
        }
    }
    
    val size = backing.size * 2
}