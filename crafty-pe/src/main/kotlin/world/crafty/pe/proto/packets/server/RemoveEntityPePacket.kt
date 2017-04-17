package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class RemoveEntityPePacket(
        val entityId: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x0F
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is RemoveEntityPePacket) throw IllegalArgumentException()
            stream.writeZigzagVarLong(obj.entityId)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return RemoveEntityPePacket(
                    entityId = stream.readZigzagVarLong()
            )
        }
    }
}