package world.crafty.pe.jwt.payloads

import world.crafty.pe.base64ToX509ec

class CertChainLink(
        val certificateAuthority: Boolean,
        val exp: Long,
        val identityPublicKey: String,
        val nbf: Long,

        val extraData: LoginExtraData?,
        val iss: String?,
        val randomNonce: Long?,
        val iat: Long?
) {
    val idPubKey by lazy { base64ToX509ec(identityPublicKey) }
}