package world.crafty.skinpool

import kotlinx.coroutines.experimental.delay
import world.crafty.mojang.MojangClient
import world.crafty.mojang.ProfileProperty
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.util.*

class SkinUploader(val uuid: UUID, val mojangClient: MojangClient) {
    init {
        require(mojangClient.authenticated) { "mojangClient must be authenticated before being used in SkinUploader" }
    }
    
    var lastUpload = Instant.MIN
    
    suspend fun uploadAsync(img: BufferedImage, slim: Boolean) : ProfileProperty {
        lastUpload = Instant.now()
        mojangClient.uploadSkinAsync(uuid, img, slim)
        delay(mojangInternalUpdate.toMillis())
        val profile = mojangClient.getFullProfileAsync(uuid)
        val texturesProp = profile.properties.first { it.name == "textures" }
        return texturesProp
    }

    companion object {
        val uploadRateLimit: Duration = Duration.ofSeconds(60)
        val mojangInternalUpdate: Duration = Duration.ofSeconds(20)
    }
}