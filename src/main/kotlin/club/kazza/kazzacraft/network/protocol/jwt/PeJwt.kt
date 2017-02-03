package club.kazza.kazzacraft.network.protocol.jwt

import club.kazza.kazzacraft.network.security.transcodeSignatureToDER
import io.vertx.core.json.Json
import java.security.Signature
import java.util.*

class PeJwt private constructor(
        val asString: String,
        val header: PeJwtHeader,
        val payload: PeJwtPayload,
        val signature: ByteArray
) {
    val isSignatureValid by lazy {
        val sigVerifier = Signature.getInstance("SHA384withECDSA")
        
        val verificationKey = header.x5uKey
        
        val toVerify = asString.substring(0 until asString.lastIndexOf('.')).toByteArray(Charsets.UTF_8)
        
        val expectedSize = 96 // for ES384
        val derSignature = if(expectedSize != signature.size && signature[0] == 0x30.toByte()) signature
            else transcodeSignatureToDER(signature)
        
        sigVerifier.initVerify(verificationKey)
        sigVerifier.update(toVerify)
        sigVerifier.verify(derSignature)
    }
    
    companion object {        
        fun parse(str: String) : PeJwt {
            val base64toStr = { str: String -> Base64.getUrlDecoder().decode(str).toString(Charsets.UTF_8) }
            val parts = str.split(".")
            return PeJwt(
                    asString = str,
                    header = Json.decodeValue(base64toStr(parts[0]), PeJwtHeader::class.java),
                    payload = Json.decodeValue(base64toStr(parts[1]), PeJwtPayload::class.java),
                    signature = Base64.getUrlDecoder().decode(parts[2])
            )
        }
    }
}