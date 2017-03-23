package world.crafty.common.kotlin

import kotlin.reflect.KProperty

fun <T> computeOnChange(watched: () -> Int, compute: () -> T): ComputeOnChange<T> = ComputeOnChange(watched, compute)

class ComputeOnChange<T>(private val watched: () -> Int, private val compute: () -> T) {
    private var value: T? = null
    private var previous: Int? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val latest = watched()
        if(previous == null || previous != latest) {
            value = compute()
        }
        return value!!
    }
}