package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

enum class ScoreboardPosition { PLAYER_LIST, SIDEBAR, BELOW_NAME }

class DisplayScoreboardPcPacket(
        val position: ScoreboardPosition,
        val name: String
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec

    companion object Codec : PcPacketCodec() {
        override val id = 0x3D
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if (obj !is DisplayScoreboardPcPacket) throw IllegalArgumentException()
            stream.writeByte(obj.position.ordinal)
            stream.writeSignedString(obj.name)
        }

        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return DisplayScoreboardPcPacket(
                    position = ScoreboardPosition.values()[stream.readByte().toInt()],
                    name = stream.readSignedString()
            )
        }
    }
}