package world.crafty.proto.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket
import world.crafty.proto.MinecraftPlatform

class SetChunkColumnCraftyPacket(
        val platform: MinecraftPlatform,
        val decompressedSize: Int,
        val zlibCompressedFullChunkPacket: ByteArray
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetChunkColumnCraftyPacket) throw IllegalArgumentException()
            stream.writeByte(obj.platform.ordinal)
            stream.writeUnsignedVarInt(obj.decompressedSize)
            stream.writeUnsignedVarInt(obj.zlibCompressedFullChunkPacket.size)
            stream.write(obj.zlibCompressedFullChunkPacket)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return SetChunkColumnCraftyPacket(
                    platform = MinecraftPlatform.values()[stream.readUnsignedByte()],
                    decompressedSize = stream.readUnsignedVarInt(),
                    zlibCompressedFullChunkPacket = stream.readByteArray(stream.readUnsignedVarInt())
            )
        }
    }
}