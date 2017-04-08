package world.crafty.common.utils

import java.util.*

fun BitSet.firstByte() : Byte {
    val bytes = toByteArray()
    return if (bytes.isEmpty()) 0 else bytes[0]
}

fun BitSet.firstLong() : Long {
    val longs = toLongArray()
    return if(longs.isEmpty()) 0L else longs[0]
}