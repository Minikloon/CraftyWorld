package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.PcLocation
import world.crafty.pc.metadata.PcMetadataMap
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.readPcLocation
import world.crafty.pc.writePcLocation
import java.util.*

class AddPlayerPcPacket(
        val entityId: Int,
        val uuid: UUID,
        val location: PcLocation,
        val metadata: PcMetadataMap
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x05
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AddPlayerPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeUuid(obj.uuid)
            stream.writePcLocation(obj.location)
            PcMetadataMap.Codec.serialize(obj.metadata, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return AddPlayerPcPacket(
                    entityId = stream.readSignedVarInt(),
                    uuid = stream.readUuid(),
                    location = stream.readPcLocation(),
                    metadata = PcMetadataMap.Codec.deserialize(stream)
            )
        }
    }
}