package world.crafty.mojang

import java.util.*

class AuthenticationPayload(
        val agent: MojangAgent,
        val username: String,
        val password: String,
        val clientToken: UUID,
        val requestUser: Boolean = false
)

class MojangAgent(
        val name: String,
        val version: Int
) {
    companion object {
        val vanillaMinecraft = MojangAgent("Minecraft", 1)
    }
}