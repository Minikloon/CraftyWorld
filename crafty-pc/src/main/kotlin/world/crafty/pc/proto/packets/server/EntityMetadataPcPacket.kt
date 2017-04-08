package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.metadata.PcMetadataMap
import world.crafty.pc.proto.PcPacket

class EntityMetadataPcPacket(
        val entityId: Int,
        val metadata: PcMetadataMap
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x39
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is EntityMetadataPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            PcMetadataMap.Codec.serialize(obj.metadata, stream)
        }

        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return EntityMetadataPcPacket(
                    entityId = stream.readSignedVarInt(),
                    metadata = PcMetadataMap.Codec.deserialize(stream)
            )
        }
    }
}