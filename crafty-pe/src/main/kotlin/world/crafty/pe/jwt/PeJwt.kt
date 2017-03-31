package world.crafty.pe.jwt

import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import world.crafty.pe.jwt.payloads.CertChainLink
import world.crafty.pe.jwt.payloads.PeClientData
import world.crafty.pe.transcodeSignatureToDER
import java.security.Signature
import java.util.*

class PeJwt private constructor(
        val asString: String,
        val header: PeJwtHeader,
        val payload: Any,
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
            val parts = str.split(".")
            
            val payloadStr = base64ToStr(parts[1])
            val payloadJson = JsonObject(payloadStr)
            val payloadClass = if(payloadJson.containsKey("exp")) CertChainLink::class else PeClientData::class 
            val payload = payloadJson.mapTo(payloadClass.java)
            
            return PeJwt(
                    asString = str,
                    header = Json.decodeValue(base64ToStr(parts[0]), PeJwtHeader::class.java),
                    payload = payload,
                    signature = Base64.getUrlDecoder().decode(parts[2])
            )
        }
        private fun base64ToStr(str: String) : String {
            return Base64.getUrlDecoder().decode(str).toString(Charsets.UTF_8)
        }
    }
}