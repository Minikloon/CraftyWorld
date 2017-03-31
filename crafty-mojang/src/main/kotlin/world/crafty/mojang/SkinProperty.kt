package world.crafty.mojang

class SkinProperty(
        val timestamp: Long,
        val profileId: String,
        val profileName: String,
        val isPublic: Boolean,
        val textures: Map<TextureType, UrlTexture>
)

enum class TextureType {
    SKIN,
    CAPE,
    ELYTRA
}

class UrlTexture(
    val url: String,
    val metadata: String?
)