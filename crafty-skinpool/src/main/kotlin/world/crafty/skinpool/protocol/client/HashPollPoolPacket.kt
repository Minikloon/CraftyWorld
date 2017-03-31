package world.crafty.skinpool.protocol.client

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

class HashPollPoolPacket(
        val hash: Long,
        val needProfile: Boolean
) {
    object Codec : McCodec<HashPollPoolPacket> {
        override fun serialize(obj: HashPollPoolPacket, stream: MinecraftOutputStream) {
            stream.writeLong(obj.hash)
            stream.writeBoolean(obj.needProfile)
        }
        override fun deserialize(stream: MinecraftInputStream): HashPollPoolPacket {
            return HashPollPoolPacket(
                    hash = stream.readLong(),
                    needProfile = stream.readBoolean()
            )
        }
    }
}