package ovh.plrapps.mapcompose.utils

fun <T> MutableCollection<T>.removeFirst(predicate: (T) -> Boolean): Boolean {
    val it = iterator()
    while (it.hasNext()) {
        if (predicate(it.next())) {
            it.remove()
            return true
        }
    }
    return false
}