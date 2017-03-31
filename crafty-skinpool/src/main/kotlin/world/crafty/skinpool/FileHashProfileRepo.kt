package world.crafty.skinpool

import io.vertx.core.json.Json
import world.crafty.mojang.ProfileProperty
import java.nio.file.Path

class FileHashProfileRepo(val path: Path) {
    private val cache = mutableMapOf<Long, ProfileProperty>()
    
    operator fun get(hash: Long) : ProfileProperty? {
        return cache[hash]
    }
    
    operator fun set(hash: Long, skin: ProfileProperty) {
        cache[hash] = skin
        path.toFile().appendText("$hash ${Json.encode(skin)}\n")
    }
    
    fun loadFromFile(path: Path) {
        path.toFile().readLines(Charsets.UTF_8).forEach { 
            val parts = it.split(" ")
            val hash = parts[0].toLong()
            val skinProp = Json.decodeValue(parts[1], ProfileProperty::class.java)
            cache[hash] = skinProp
        }
    }
}