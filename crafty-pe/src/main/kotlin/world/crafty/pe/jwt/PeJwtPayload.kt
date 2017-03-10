package world.crafty.pe.jwt

import world.crafty.pe.base64ToX509ec

data class PeJwtPayload(
        val exp: Long,
        val iss: String?,
        val extraData: LoginExtraData,
        val identityPublicKey: String,
        val nbf: Long
) {
    val idPubKey by lazy { base64ToX509ec(identityPublicKey) }
}