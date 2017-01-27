package club.kazza.kazzacraft.network.protocol

import club.kazza.kazzacraft.utils.kotlin.lazyIf

class SplittedMessage(val id: Int, splits: Int, val timestamp: Long = System.currentTimeMillis()) {
    private val parts: Array<ByteArray?> = Array(splits) { null }
    
    val isComplete: Boolean
        get() = parts.count { it != null } == parts.size
    
    // returns null if isComplete == false 
    val full: ByteArray? by lazyIf(
            condition = { isComplete },
            otherwise = { null },
            initializer = { 
                val combined = ByteArray(parts.sumBy { it!!.size })
                var index = 0
                parts.forEach { 
                    System.arraycopy(it, 0, combined, index, it!!.size)
                    index += it.size
                }
                combined
            }
    )
    
    fun addSplit(index: Int, data: ByteArray) {
        parts[index] = data
    }
}