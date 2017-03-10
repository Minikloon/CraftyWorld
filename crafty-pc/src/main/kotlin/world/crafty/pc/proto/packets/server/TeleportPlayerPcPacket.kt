package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.world.Location

class TeleportPlayerPcPacket(
        val loc: Location,
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
            stream.writeFloat(obj.loc.yaw)
            stream.writeFloat(obj.loc.pitch)
            stream.writeByte(obj.relativeFlags)
            stream.writeUnsignedVarInt(obj.confirmId)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return TeleportPlayerPcPacket(
                    loc = Location(
                            x = stream.readDouble(),
                            y = stream.readDouble(),
                            z = stream.readDouble(),
                            yaw = stream.readFloat(),
                            pitch = stream.readFloat()
                    ),
                    relativeFlags = stream.readByte().toInt(),
                    confirmId = stream.readUnsignedVarInt()
            )
        }
    }
}