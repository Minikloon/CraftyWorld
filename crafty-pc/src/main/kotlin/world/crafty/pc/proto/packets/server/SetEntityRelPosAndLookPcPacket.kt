package world.crafty.pc.proto.packets.server

import world.crafty.common.Angle256
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class SetEntityRelPosAndLookPcPacket(
        val entityId: Int,
        val dx: Int,
        val dy: Int,
        val dz: Int,
        val yaw: Angle256,
        val pitch: Angle256,
        val onGround: Boolean
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x28
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetEntityRelPosAndLookPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeShort(obj.dx)
            stream.writeShort(obj.dy)
            stream.writeShort(obj.dz)
            stream.writeAngle(obj.yaw)
            stream.writeAngle(obj.pitch)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SetEntityRelPosAndLookPcPacket(
                    entityId = stream.readSignedVarInt(),
                    dx = stream.readShort().toInt(),
                    dy = stream.readShort().toInt(),
                    dz = stream.readShort().toInt(),
                    yaw = stream.readAngle(),
                    pitch = stream.readAngle(),
                    onGround = stream.readBoolean()
            )
        }
    }
}