package club.kazza.kazzacraft.utils.kotlin

fun <T : Any> MutableList<T>.firstOrCompute(predicate: (T) -> Boolean, supplier: () -> T) : T {
    var elem = firstOrNull(predicate)
    if(elem == null) {
        elem = supplier()
        add(elem)
    }
    return elem
}