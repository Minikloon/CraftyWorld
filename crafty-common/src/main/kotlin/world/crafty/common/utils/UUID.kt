package world.crafty.common.utils

import java.util.*

fun uuidFromNoHyphens(str: String) : UUID {
    val withHyphens = str.substring(0, 8) + "-" + str.substring(8, 12) + "-" + str.substring(12, 16) + "-" + str.substring(16, 20) + "-" + str.substring(20, 32)
    return UUID.fromString(withHyphens)
}