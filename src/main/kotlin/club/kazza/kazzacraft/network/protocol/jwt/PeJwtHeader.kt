package club.kazza.kazzacraft.network.protocol.jwt

import club.kazza.kazzacraft.network.security.base64ToX509ec
import java.security.PublicKey

data class PeJwtHeader(
        val alg: String, 
        val x5u: String
) {
    val x5uKey: PublicKey by lazy { base64ToX509ec(x5u) }
}