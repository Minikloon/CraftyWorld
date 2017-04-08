package world.crafty.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket
import world.crafty.proto.metadata.MetaValue

class PatchEntityCraftyPacket(
        val entityId: Long,
        val values: List<MetaValue>
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PatchEntityCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            MetaValue.serialize(obj.values, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return PatchEntityCraftyPacket(
                    entityId = stream.readUnsignedVarLong(),
                    values = MetaValue.deserialize(stream)
            )
        }
    }
}