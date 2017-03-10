package world.crafty.common.utils

import java.util.*

class EvictingQueue<E>(val maxSize: Int) : LinkedList<E>() {
    override fun add(element: E): Boolean {
        if(size + 1 >= maxSize)
            remove()
        return super.add(element)
    }
    
    override fun offer(e: E): Boolean {
        if(size + 1 >= maxSize)
            remove()
        return super.add(e)
    }
} 