package world.crafty.skinpool.protocol.server

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.mojang.ProfileProperty

class HashPollReplyPoolPacket(
        val hash: Long,
        val hasProfile: Boolean,
        val textureProp: ProfileProperty?
) {
    object Codec : McCodec<HashPollReplyPoolPacket> {
        override fun serialize(obj: HashPollReplyPoolPacket, stream: MinecraftOutputStream) {
            stream.writeLong(obj.hash)
            stream.writeBoolean(obj.hasProfile)
            stream.writeBoolean(obj.textureProp != null)
            if(obj.textureProp != null)
                stream.writeJson(obj.textureProp)
        }
        override fun deserialize(stream: MinecraftInputStream): HashPollReplyPoolPacket {
            return HashPollReplyPoolPacket(
                    hash = stream.readLong(),
                    hasProfile = stream.readBoolean(),
                    textureProp = if(stream.readBoolean()) stream.readJson(ProfileProperty::class.java) else null
            )
        }
    }
}