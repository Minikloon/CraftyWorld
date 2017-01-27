package club.kazza.kazzacraft.network.raknet

import java.util.*

class OrderingChannel(val channelId: Byte, private var expectedIndex: Int = 0) {
    private val queue: PriorityQueue<RakMessage> = PriorityQueue(10, Comparator({a , b ->
            Integer.compare(a.order?.index ?: 0, b.order?.index ?: 0)
    }))
    
    fun add(message: RakMessage) {
        require(message.order != null)
        require(message.order?.channel == channelId)
        queue.add(message)
    }
    
    fun poll() : RakMessage? {
        val message = queue.poll() ?: return null
        if(message.order?.index == expectedIndex) {
            ++expectedIndex
            return message
        }
        return null
    }
}