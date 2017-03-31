package world.crafty.pe

import world.crafty.pe.jwt.PeJwt
import world.crafty.pe.jwt.payloads.CertChainLink
import java.security.KeyFactory
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.*

fun isCertChainValid(chain: List<PeJwt>) : Boolean {
    if(chain.isEmpty()) return false
    for(i in 0 until (chain.size - 1)) {
        val payload = chain[i].payload as CertChainLink
        if(payload.idPubKey != chain[i+1].header.x5uKey)
            return false
    }
    return chain.none { !it.isSignatureValid }
}

fun base64ToX509ec(str: String) : PublicKey {
    val bytes = Base64.getDecoder().decode(str)
    return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
}

// comes from https://github.com/jwtk/jjwt/blob/61510dfca58dd40b4b32c708935126785dcff48c/src/main/java/io/jsonwebtoken/impl/crypto/EllipticCurveProvider.java
fun transcodeSignatureToDER(jwsSignature: ByteArray): ByteArray {
    val rawLen = jwsSignature.size / 2

    var i = rawLen

    while (i > 0 && jwsSignature[rawLen - i].toInt() == 0)
        i--

    var j = i

    if (jwsSignature[rawLen - i] < 0) {
        j += 1
    }

    var k = rawLen

    while (k > 0 && jwsSignature[2 * rawLen - k].toInt() == 0)
        k--

    var l = k

    if (jwsSignature[2 * rawLen - k] < 0) {
        l += 1
    }

    val len = 2 + j + 2 + l

    if (len > 255) {
        throw Exception("Invalid ECDSA signature format")
    }

    var offset: Int

    val derSignature: ByteArray

    if (len < 128) {
        derSignature = ByteArray(2 + 2 + j + 2 + l)
        offset = 1
    } else {
        derSignature = ByteArray(3 + 2 + j + 2 + l)
        derSignature[1] = 0x81.toByte()
        offset = 2
    }

    derSignature[0] = 48
    derSignature[offset++] = len.toByte()
    derSignature[offset++] = 2
    derSignature[offset++] = j.toByte()

    System.arraycopy(jwsSignature, rawLen - i, derSignature, offset + j - i, i)

    offset += j

    derSignature[offset++] = 2
    derSignature[offset++] = l.toByte()

    System.arraycopy(jwsSignature, 2 * rawLen - k, derSignature, offset + l - k, k)

    return derSignature
}

private val rand = SecureRandom()
fun generateBytes(size: Int) : ByteArray {
    val bytes = ByteArray(size)
    rand.nextBytes(bytes)
    return bytes
}