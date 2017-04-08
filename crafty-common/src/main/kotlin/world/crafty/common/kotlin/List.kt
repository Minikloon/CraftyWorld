package world.crafty.common.kotlin

fun <T : Any> MutableList<T>.firstOrCompute(predicate: (T) -> Boolean, supplier: () -> T) : T {
    var elem = firstOrNull(predicate)
    if(elem == null) {
        elem = supplier()
        add(elem)
    }
    return elem
}

fun <T: Any> flatListOf(vararg lists: List<T>) : List<T> {
    val size = lists.sumBy { it.size }
    val fullList = ArrayList<T>(size)
    lists.forEach { fullList.addAll(it) }
    return fullList
}