package world.crafty.mojang

import world.crafty.common.utils.uuidFromNoHyphens
import java.util.*

class MojangProfile(
        val id: String,
        val name: String,
        val properties: List<ProfileProperty>
) {
    constructor(uuid: UUID, name: String, properties: List<ProfileProperty>)
        : this(uuid.toString().replace("-", ""), name, properties)
    
    val uuid by lazy { uuidFromNoHyphens(id) }
}