package world.crafty.pe.proto.packets.mixed

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.PeLocation
import world.crafty.pe.proto.PePacket
import world.crafty.pe.readPeLocationFloatAngles
import world.crafty.pe.writePeLocationFloatAngles

enum class MoveMode { INTERPOLATE, TELEPORT, ROTATION }
class SetPlayerLocPePacket(
        val entityId: Long,
        val loc: PeLocation,
        val mode: MoveMode,
        val onGround: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x14
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetPlayerLocPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writePeLocationFloatAngles(obj.loc)
            stream.writeByte(obj.mode.ordinal)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetPlayerLocPePacket(
                    entityId = stream.readUnsignedVarLong(),
                    loc = stream.readPeLocationFloatAngles(),
                    mode = MoveMode.values()[stream.readByte().toInt()],
                    onGround = stream.readBoolean()
            )
        }
    }
}