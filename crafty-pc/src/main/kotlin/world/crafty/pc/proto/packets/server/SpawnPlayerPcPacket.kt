package world.crafty.pc.proto.packets.server

import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.metadata.PcMetadataMap
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.readPcLocation
import world.crafty.pc.writePcLocation
import java.util.*

class SpawnPlayerPcPacket(
        val entityId: Int,
        val uuid: UUID,
        val location: Location,
        val metadata: PcMetadataMap
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x05
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SpawnPlayerPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeUuid(obj.uuid)
            stream.writePcLocation(obj.location)
            PcMetadataMap.Codec.serialize(obj.metadata, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SpawnPlayerPcPacket(
                    entityId = stream.readSignedVarInt(),
                    uuid = stream.readUuid(),
                    location = stream.readPcLocation(),
                    metadata = PcMetadataMap.Codec.deserialize(stream)
            )
        }
    }
}