package world.crafty.mojang

import world.crafty.common.utils.uuidFromNoHyphens

class MojangProfile(
        val id: String,
        val name: String,
        val properties: List<ProfileProperty>
) {
    val uuid by lazy { uuidFromNoHyphens(id) }
}