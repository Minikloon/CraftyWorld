package world.crafty.mojang

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.multipart.MemoryFileUpload
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import world.crafty.common.vertx.encodeMultipart
import world.crafty.common.vertx.vxHttp
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javax.imageio.ImageIO

class MojangClient(vertx: Vertx) {
    private val https = WebClient.create(vertx, WebClientOptions().setSsl(true))
    private val sessionServer = "sessionserver.mojang.com"
    private val authServer = "authserver.mojang.com"
    private val apiServer = "api.mojang.com"
    
    private val clientUuid = UUID.randomUUID()
    private var accessToken: String? = null
    
    val authenticated: Boolean
        get() { return accessToken != null }

    suspend fun checkHasJoinedAsync(username: String, serverId: String) : MojangProfile {
        val response = vxHttp {
            https.get(443, sessionServer, "/session/minecraft/hasJoined?username=$username&serverId=$serverId")
                    .send(it)
        }
        verifyMojangError(response)
        return response.bodyAsJson(MojangProfile::class.java)
    }
    
    suspend fun getFullProfileAsync(uuid: UUID) : MojangProfile {
        val response = vxHttp {
            https.get(443, sessionServer, "/session/minecraft/profile/${noHyphens(uuid)}?unsigned=false")
                    .send(it)
        }
        verifyMojangError(response)
        return response.bodyAsJson(MojangProfile::class.java)
    }
    
    suspend fun authenticateAsync(username: String, password: String) {
        val payload = AuthenticationPayload(
                agent = MojangAgent.vanillaMinecraft,
                username = username,
                password = password,
                clientToken = clientUuid,
                requestUser = false
        )
        
        val response = vxHttp {
            https.post(443, authServer, "/authenticate")
                    .putHeader("Content-Type", "application/json")
                    .sendJson(payload, it)
        }

        verifyMojangError(response)

        val json = JsonObject(response.bodyAsString())
        accessToken = json.getString("accessToken")
    }
    
    suspend fun uploadSkinAsync(uuid: UUID, image: BufferedImage, slim: Boolean) {
        if(accessToken == null)
            throw IllegalStateException("You have to authenticate before using this method!")
        
        val bs = ByteArrayOutputStream(64*64)
        ImageIO.write(image, "png", bs)
        val skinPngBytes = bs.toByteArray()
        
        val requestElements = encodeMultipart { encoder ->
            val skinType = if(slim) "slim" else ""
            encoder.addBodyAttribute("model", skinType)
            val fileUpload = MemoryFileUpload("file", "skin.png", "image/png", null, null, skinPngBytes.size.toLong())
            fileUpload.setContent(Unpooled.wrappedBuffer(skinPngBytes))
            encoder.addBodyHttpData(fileUpload)
        }

        vxHttp {
            val request = https.put(443, apiServer, "/user/profile/${noHyphens(uuid)}/skin")
            request.putHeader("Authorization", "Bearer $accessToken")
            requestElements.template.headers().forEach { entry ->
                request.putHeader(entry.key, entry.value)
            }
            request.sendBuffer(requestElements.buffer, it)
        }
    }
    
    private fun verifyMojangError(response: HttpResponse<out Any>) {
        val status = response.statusCode()
        if(status != 200 && status != 204)
            throw Exception("Mojang API Error: ${response.statusCode()} ${response.statusMessage()}")
    }
    
    private fun noHyphens(uuid: UUID) : String {
        return uuid.toString().replace("-", "")
    }
    
    companion object {
        val requestsPerMinute = 60

        fun getServerIdHash(sessionId: Int, sharedSecret: SecretKeySpec, pubKey: ByteArray) : String {
            val serverIdDigest = MessageDigest.getInstance("SHA-1")
            serverIdDigest.update("$sessionId".toByteArray())
            serverIdDigest.update(sharedSecret.encoded)
            serverIdDigest.update(pubKey)
            return BigInteger(serverIdDigest.digest()).toString(16)
        }
    }
}