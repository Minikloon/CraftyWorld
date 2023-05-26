package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

enum class ScoreboardObjectiveAction(val hasText: Boolean) { CREATE(true), REMOVE(false), UPDATE(true) }

class ScoreboardObjectivePcPacket(
        val name: String,
        val mode: ScoreboardObjectiveAction,
        val text: String?,
        val type: Int?
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec

    companion object Codec : PcPacketCodec() {
        override val id = 0x44
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if (obj !is ScoreboardObjectivePcPacket) throw IllegalArgumentException()
            stream.writeSignedString(obj.name)
            stream.writeByte(obj.mode.ordinal)
            if(obj.mode.hasText) {
                stream.writeSignedString(obj.text!!)
                stream.writeSignedVarInt(obj.type!!)
            }
        }

        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            val name = stream.readSignedString()
            val mode = ScoreboardObjectiveAction.values()[stream.readByte().toInt()]
            return ScoreboardObjectivePcPacket(
                    name = name,
                    mode = mode,
                    text = if(mode.hasText) stream.readSignedString() else null,
                    type = if(mode.hasText) stream.readSignedVarInt() else null
            )
        }
    }
}