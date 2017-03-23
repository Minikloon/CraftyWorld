package world.crafty.common.utils

private val FNV1_32_INIT = 0x811c9dc5.toInt()
private val FNV1_PRIME_32 = 16777619
private val FNV1_64_INIT = java.lang.Long.parseUnsignedLong("cbf29ce484222325", 16) // work around kotlin literals
private val FNV1_PRIME_64 = 1099511628211L

fun hashFnv1a64(vararg arrays: ByteArray): Long {
    var hash = FNV1_64_INIT
    for (array in arrays) {
        for (byte in array) {
            hash = hash xor (byte.toInt() and 0xFF).toLong()
            hash *= FNV1_PRIME_64
        }
    }
    return hash
}