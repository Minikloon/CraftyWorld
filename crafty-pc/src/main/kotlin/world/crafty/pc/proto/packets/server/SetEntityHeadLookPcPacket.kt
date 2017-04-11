package world.crafty.pc.proto.packets.server

import world.crafty.common.Angle256
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class SetEntityHeadLookPcPacket(
        val entityId: Int,
        val headYaw: Angle256
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x34
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetEntityHeadLookPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeAngle(obj.headYaw)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SetEntityHeadLookPcPacket(
                    entityId = stream.readSignedVarInt(),
                    headYaw = stream.readAngle()
            )
        }
    }
}