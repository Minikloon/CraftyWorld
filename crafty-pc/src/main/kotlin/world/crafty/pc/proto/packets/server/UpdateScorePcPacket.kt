package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

enum class UpdateScoreAction { UPSERT, REMOVE }

class UpdateScorePcPacket(
        val entityName: String,
        val action: UpdateScoreAction,
        val objectiveName: String,
        val value: Int?
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec

    companion object Codec : PcPacketCodec() {
        override val id = 0x47
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is UpdateScorePcPacket) throw IllegalArgumentException()
            stream.writeSignedString(obj.entityName)
            stream.writeByte(obj.action.ordinal)
            stream.writeSignedString(obj.objectiveName)
            if(obj.action == UpdateScoreAction.UPSERT) {
                stream.writeSignedVarInt(obj.value!!)
            }
        }

        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            val entityName = stream.readSignedString()
            val action = UpdateScoreAction.values()[stream.readByte().toInt()]
            return UpdateScorePcPacket(
                    entityName = entityName,
                    action = action,
                    objectiveName = stream.readSignedString(),
                    value = if(action == UpdateScoreAction.UPSERT) stream.readSignedVarInt() else null
            )
        }
    }
}