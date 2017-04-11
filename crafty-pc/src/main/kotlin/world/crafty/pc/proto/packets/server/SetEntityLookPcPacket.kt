package world.crafty.pc.proto.packets.server

import world.crafty.common.Angle256
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class SetEntityLookPcPacket(
        val entityId: Int,
        val yaw: Angle256,
        val pitch: Angle256,
        val onGround: Boolean
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x27
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetEntityLookPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeAngle(obj.yaw)
            stream.writeAngle(obj.pitch)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SetEntityLookPcPacket(
                    entityId = stream.readSignedVarInt(),
                    yaw = stream.readAngle(),
                    pitch = stream.readAngle(),
                    onGround = stream.readBoolean()
            )
        }
    }
}