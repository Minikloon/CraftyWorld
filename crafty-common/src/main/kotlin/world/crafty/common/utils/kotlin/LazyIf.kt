package world.crafty.common.utils.kotlin

import kotlin.reflect.KProperty

fun <T> lazyIf(condition: () -> Boolean, otherwise: () -> T, initializer: () -> T): LazyIf<T> = LazyIf(condition, otherwise, initializer)

class LazyIf<T>(private val condition: () -> Boolean, private val otherwise: () -> T, initializer: () -> T) {
    private val innerLazy = lazy(initializer)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(condition()) {
            return innerLazy.getValue(thisRef, property)
        } else {
            return otherwise()
        }
    }
}