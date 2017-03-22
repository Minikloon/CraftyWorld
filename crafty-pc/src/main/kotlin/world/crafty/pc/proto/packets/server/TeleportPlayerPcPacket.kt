package world.crafty.pc.proto.packets.server

import world.crafty.common.Angle256
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket
import world.crafty.common.Location
import world.crafty.pc.PcLocation

class TeleportPlayerPcPacket(
        val loc: PcLocation,
        val relativeFlags: Int,
        val confirmId: Int
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x2E
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is TeleportPlayerPcPacket) throw IllegalArgumentException()
            stream.writeDouble(obj.loc.x)
            stream.writeDouble(obj.loc.y)
            stream.writeDouble(obj.loc.z)
            stream.writeFloat(obj.loc.yaw.toDegrees())
            stream.writeFloat(obj.loc.pitch.toDegrees())
            stream.writeByte(obj.relativeFlags)
            stream.writeSignedVarInt(obj.confirmId)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return TeleportPlayerPcPacket(
                    loc = PcLocation(
                            x = stream.readDouble(),
                            y = stream.readDouble(),
                            z = stream.readDouble(),
                            yaw = Angle256.fromDegrees(stream.readFloat()),
                            pitch = Angle256.fromDegrees(stream.readFloat())
                    ),
                    relativeFlags = stream.readByte().toInt(),
                    confirmId = stream.readSignedVarInt()
            )
        }
    }
}