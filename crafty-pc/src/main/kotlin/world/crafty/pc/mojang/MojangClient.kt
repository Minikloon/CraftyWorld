package world.crafty.pc.mojang

import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.json.JsonObject
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.experimental.suspendCoroutine

class MojangClient(vertx: Vertx) {
    private val https: HttpClient = vertx.createHttpClient(HttpClientOptions().setSsl(true))

    fun getServerIdHash(sessionId: String, sharedSecret: SecretKeySpec, pubKey: ByteArray) : String {
        val serverIdDigest = MessageDigest.getInstance("SHA-1")
        serverIdDigest.update(sessionId.toByteArray())
        serverIdDigest.update(sharedSecret.encoded)
        serverIdDigest.update(pubKey)
        return BigInteger(serverIdDigest.digest()).toString(16)
    }

    suspend fun checkHasJoinedAsync(username: String, serverId: String) = suspendCoroutine<MojangProfile> { c ->
        https.getNow(443, "sessionserver.mojang.com", "/session/minecraft/hasJoined?username=$username&serverId=$serverId", {
            it.bodyHandler {
                val json = JsonObject(it.toString())
                val profile = parseProfile(json)
                c.resume(profile)
            }.exceptionHandler {
                c.resumeWithException(it)
            }
        })
    }

    private fun parseProfile(json: JsonObject) : MojangProfile {
        return MojangProfile(
                uuid = uuidFromNoHyphens(json.getString("id")),
                name = json.getString("name"),
                properties = listOf(
                        ProfileProperty(
                                name = "textures",
                                value = json.getJsonArray("properties").getJsonObject(0).getString("value"),
                                signature = json.getJsonArray("properties").getJsonObject(0).getString("signature")
                        )
                )
        )
    }

    private fun uuidFromNoHyphens(str: String) : UUID {
        val withHyphens = str.substring(0, 8) + "-" + str.substring(8, 12) + "-" + str.substring(12, 16) + "-" + str.substring(16, 20) + "-" + str.substring(20, 32)
        return UUID.fromString(withHyphens)
    }
}