package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket
import world.crafty.common.Location
import world.crafty.pc.readPcLocationFloatAngles
import world.crafty.pc.writePcLocationFloatAngles

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
            stream.writePcLocationFloatAngles(obj.loc)
            stream.writeByte(obj.relativeFlags)
            stream.writeSignedVarInt(obj.confirmId)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return TeleportPlayerPcPacket(
                    loc = stream.readPcLocationFloatAngles(),
                    relativeFlags = stream.readByte().toInt(),
                    confirmId = stream.readSignedVarInt()
            )
        }
    }
}