package club.kazza.kazzacraft.network.protocol.jwt

data class LoginExtraData(
        val displayName: String,
        val identity: String
)