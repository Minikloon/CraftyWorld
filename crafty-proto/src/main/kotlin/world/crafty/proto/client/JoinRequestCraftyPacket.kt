package world.crafty.proto.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class JoinRequestCraftyPacket(
        val username: String,
        val authMojang: Boolean,
        val authXbox: Boolean
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is JoinRequestCraftyPacket) throw IllegalArgumentException()
            stream.writeString(obj.username)
            stream.writeBoolean(obj.authMojang)
            stream.writeBoolean(obj.authXbox)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return JoinRequestCraftyPacket(
                    username = stream.readString(),
                    authMojang = stream.readBoolean(),
                    authXbox = stream.readBoolean()
            )
        }
    }
}