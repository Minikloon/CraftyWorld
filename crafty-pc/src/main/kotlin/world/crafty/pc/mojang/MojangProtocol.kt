package world.crafty.pc.mojang

import java.util.*

data class MojangProfile(
        val uuid: UUID,
        val name: String,
        val properties: List<ProfileProperty>
)

data class ProfileProperty(
        val name: String,
        val value: String,
        val signature: String?
)