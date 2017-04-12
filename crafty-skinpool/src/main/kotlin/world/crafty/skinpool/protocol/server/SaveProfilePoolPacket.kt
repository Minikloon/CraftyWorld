package world.crafty.skinpool.protocol.server

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.mojang.ProfileProperty

internal class SaveProfilePoolPacket(
        val hash: Long,
        val textureProperty: ProfileProperty
) {
    object Codec : McCodec<SaveProfilePoolPacket> {
        override fun serialize(obj: SaveProfilePoolPacket, stream: MinecraftOutputStream) {
            stream.writeLong(obj.hash)
            stream.writeJson(obj.textureProperty)
        }
        override fun deserialize(stream: MinecraftInputStream): SaveProfilePoolPacket {
            return SaveProfilePoolPacket(
                    hash = stream.readLong(),
                    textureProperty = stream.readJson(ProfileProperty::class)
            )
        }
    }
}