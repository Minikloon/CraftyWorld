package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class SetDifficultyPePacket(
        val difficulty: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x3d
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetDifficultyPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.difficulty)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetDifficultyPePacket(
                    difficulty = stream.readUnsignedVarInt()
            )
        }
    }
}