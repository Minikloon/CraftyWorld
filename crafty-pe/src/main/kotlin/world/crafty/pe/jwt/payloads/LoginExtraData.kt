package world.crafty.pe.jwt.payloads

data class LoginExtraData(
        val displayName: String,
        val identity: String,
        val XUID: String?
)