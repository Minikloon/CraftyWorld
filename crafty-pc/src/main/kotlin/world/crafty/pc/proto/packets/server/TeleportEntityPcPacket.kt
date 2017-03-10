package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class TeleportEntityPcPacket(
        val entityId: Int,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Byte,
        val pitch: Byte,
        val onGround: Boolean
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x49
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is TeleportEntityPcPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.entityId)
            stream.writeDouble(obj.x)
            stream.writeDouble(obj.y)
            stream.writeDouble(obj.z)
            stream.writeByte(obj.yaw)
            stream.writeByte(obj.pitch)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return TeleportEntityPcPacket(
                    entityId = stream.readUnsignedVarInt(),
                    x = stream.readDouble(),
                    y = stream.readDouble(),
                    z = stream.readDouble(),
                    yaw = stream.readByte(),
                    pitch = stream.readByte(),
                    onGround = stream.readBoolean()
            )
        }
    }
}