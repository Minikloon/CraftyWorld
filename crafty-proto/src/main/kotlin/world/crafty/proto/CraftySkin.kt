package world.crafty.proto

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

class CraftySkin(
        val fnvHashOfPng: Long,
        val pngBytes: ByteArray
) {
    object Codec : McCodec<CraftySkin> {
        override fun serialize(obj: CraftySkin, stream: MinecraftOutputStream) {
            stream.writeLong(obj.fnvHashOfPng)
            stream.writeUnsignedVarInt(obj.pngBytes.size)
            stream.write(obj.pngBytes)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftySkin {
            return CraftySkin(
                    fnvHashOfPng = stream.readLong(),
                    pngBytes = stream.readBytes(stream.readUnsignedVarInt())
            )
        }
    }
}