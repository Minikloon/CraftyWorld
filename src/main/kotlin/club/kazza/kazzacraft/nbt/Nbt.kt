package club.kazza.kazzacraft.nbt

import kotlin.reflect.KClass

object Nbt {
    fun encode(obj: Any) : ByteArray {

    }

    fun <T: Any> decode(bytes: ByteArray, clazz: KClass<T>) : T {
        
    }

    fun prettyString(obj: Any) : String {

    }
}