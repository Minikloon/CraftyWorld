package world.crafty.pe.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class ChunkRadiusRequestPePacket(
        val desiredChunkRadius: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x44
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ChunkRadiusRequestPePacket) throw IllegalArgumentException()
            stream.writeZigzagVarInt(id)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ChunkRadiusRequestPePacket(
                    desiredChunkRadius = stream.readZigzagVarInt()
            )
        }
    }
}