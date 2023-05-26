package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class DestroyEntitiesPcPacket(
        val entityIds: Collection<Int>
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x34
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is DestroyEntitiesPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityIds.size)
            obj.entityIds.forEach { 
                stream.writeSignedVarInt(it)
            }
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return DestroyEntitiesPcPacket(
                    entityIds = (1..stream.readSignedVarInt()).map { stream.readSignedVarInt() }
            )
        }
    }
}