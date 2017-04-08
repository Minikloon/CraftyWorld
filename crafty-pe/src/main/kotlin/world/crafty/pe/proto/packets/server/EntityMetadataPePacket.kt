package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.metadata.PeMetadataMap
import world.crafty.pe.proto.PePacket

class EntityMetadataPePacket(
        val entityId: Long,
        val metadata: PeMetadataMap
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x28
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is EntityMetadataPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            PeMetadataMap.Codec.serialize(obj.metadata, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return EntityMetadataPePacket(
                    entityId = stream.readUnsignedVarLong(),
                    metadata = PeMetadataMap.Codec.deserialize(stream)
            )
        }
    }
}