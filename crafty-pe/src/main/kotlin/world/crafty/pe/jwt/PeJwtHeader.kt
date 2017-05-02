package world.crafty.pe.jwt

import world.crafty.pe.base64ToX509ec
import java.security.PublicKey

data class PeJwtHeader(
        val alg: String, 
        val x5u: String
) {
    val x5uKey: PublicKey by lazy { base64ToX509ec(x5u) }
}