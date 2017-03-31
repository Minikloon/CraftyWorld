package world.crafty.skinpool.protocol.client

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.decompressed

class SaveSkinPoolPacket(
        val hash: Long,
        val slim: Boolean,
        val skinPng: ByteArray
) {
    object Codec : McCodec<SaveSkinPoolPacket> {
        override fun serialize(obj: SaveSkinPoolPacket, stream: MinecraftOutputStream) {
            stream.writeLong(obj.hash)
            stream.writeBoolean(obj.slim)
            stream.writeUnsignedVarInt(obj.skinPng.size)
            stream.write(obj.skinPng)
        }
        override fun deserialize(stream: MinecraftInputStream): SaveSkinPoolPacket {
            return SaveSkinPoolPacket(
                    hash = stream.readLong(),
                    slim = stream.readBoolean(),
                    skinPng = stream.readBytes(stream.readUnsignedVarInt()).decompressed(CompressionAlgorithm.ZLIB)
            )
        }
    }
}