package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class SetDifficultyPcPacket(
        val difficulty: Int
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x0D
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetDifficultyPcPacket) throw IllegalArgumentException()
            stream.writeByte(obj.difficulty)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SetDifficultyPcPacket(
                    difficulty = stream.readUnsignedByte()
            )
        }
    }
}