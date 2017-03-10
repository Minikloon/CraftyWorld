package world.crafty.pe.proto.packets.mixed

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

enum class MoveMode { INTERPOLATE, TELEPORT, ROTATION }
class SetPlayerPositionPePacket(
        val entityId: Long,
        val x: Float,
        val y: Float,
        val z: Float,
        val headPitch: Float,
        val headYaw: Float,
        val bodyYaw: Float,
        val mode: MoveMode,
        val onGround: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x14
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetPlayerPositionPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeFloatLe(obj.x)
            stream.writeFloatLe(obj.y)
            stream.writeFloatLe(obj.z)
            stream.writeFloatLe(obj.headPitch)
            stream.writeFloatLe(obj.headYaw)
            stream.writeFloatLe(obj.bodyYaw)
            stream.writeByte(obj.mode.ordinal)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetPlayerPositionPePacket(
                    entityId = stream.readUnsignedVarLong(),
                    x = stream.readFloatLe(),
                    y = stream.readFloatLe(),
                    z = stream.readFloatLe(),
                    headPitch = stream.readFloatLe(),
                    headYaw = stream.readFloatLe(),
                    bodyYaw = stream.readFloatLe(),
                    mode = MoveMode.values()[stream.readByte().toInt()],
                    onGround = stream.readBoolean()
            )
        }
    }
}