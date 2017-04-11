package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class SetEntityRelPosPcPacket(
        val entityId: Int,
        val dx: Int,
        val dy: Int,
        val dz: Int,
        val onGround: Boolean
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x25
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetEntityRelPosPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeShort(obj.dx)
            stream.writeShort(obj.dy)
            stream.writeShort(obj.dz)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SetEntityRelPosPcPacket(
                    entityId = stream.readSignedVarInt(),
                    dx = stream.readShort().toInt(),
                    dy = stream.readShort().toInt(),
                    dz = stream.readShort().toInt(),
                    onGround = stream.readBoolean()
            )
        }
    }
}