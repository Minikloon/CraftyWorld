package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class SetChunkRadiusPePacket(
        val chunkRadius: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x46
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetChunkRadiusPePacket) throw IllegalArgumentException()
            stream.writeZigzagVarInt(obj.chunkRadius)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetChunkRadiusPePacket(
                    chunkRadius = stream.readZigzagVarInt()
            )
        }
    }
}