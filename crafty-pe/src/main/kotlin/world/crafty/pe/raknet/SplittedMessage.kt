package world.crafty.pe.raknet

import world.crafty.common.kotlin.lazyIf

class SplittedMessage(val id: Short, splits: Int, val timestamp: Long = System.currentTimeMillis()) {
    private val parts: Array<RakMessage?> = Array(splits) { null }

    val isComplete: Boolean
        get() = {
            val count = parts.count { it != null }
            count == parts.size
        }.invoke()

    // returns null if isComplete == false 
    val full: RakMessage? by lazyIf(
            condition = { isComplete },
            otherwise = { null },
            initializer = {
                val combinedData = ByteArray(parts.sumBy { it!!.data.size })
                var index = 0
                parts.forEach {
                    val data = it!!.data
                    System.arraycopy(data, 0, combinedData, index, data.size)
                    index += data.size
                }
                val last = parts.last() as RakMessage
                val header = RakMessageFlags(last.headerFlags.reliability, false)
                RakMessage(header, last.reliability, last.order, null, combinedData)
            }
    )

    fun addSplit(index: Int, split: RakMessage) {
        parts[index] = split
    }
}