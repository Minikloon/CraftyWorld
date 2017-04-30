package world.crafty.common.utils

import io.vertx.core.json.Json
import kotlin.reflect.KClass

fun <T: Any> Map<String, Any>.reflectMap(clazz: KClass<T>) : T {
    return Json.mapper.convertValue(this, clazz.java)
}