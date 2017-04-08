package world.crafty.proto.packets.client

import world.crafty.proto.MinecraftPlatform
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket
import world.crafty.proto.CraftySkin
import java.util.*

class JoinRequestCraftyPacket(
        val username: String,
        val authMojang: Boolean,
        val authXbox: Boolean,
        val platform: MinecraftPlatform,
        val skin: CraftySkin
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is JoinRequestCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedString(obj.username)
            stream.writeBoolean(obj.authMojang)
            stream.writeBoolean(obj.authXbox)
            stream.writeByte(obj.platform.ordinal)
            CraftySkin.Codec.serialize(obj.skin, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return JoinRequestCraftyPacket(
                    username = stream.readUnsignedString(),
                    authMojang = stream.readBoolean(),
                    authXbox = stream.readBoolean(),
                    platform = MinecraftPlatform.values()[stream.readUnsignedByte()],
                    skin = CraftySkin.Codec.deserialize(stream)
            )
        }
    }
}