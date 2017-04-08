package world.crafty.common.kotlin

fun <K, V> flatMapOf(vararg maps: Map<K, V>) : Map<K, V> {
    val fullMap = HashMap<K, V>()
    maps.forEach { 
        fullMap.putAll(it)
    }
    return fullMap
}

fun <K, V> flatMapOf(maps: List<Map<K, V>>) : Map<K ,V> {
    return mutableFlatMapOf(maps)
}

fun <K, V> mutableFlatMapOf(maps: List<Map<K, V>>) : MutableMap<K, V> {
    val fullMap = HashMap<K, V>()
    maps.forEach {
        fullMap.putAll(it)
    }
    return fullMap
}