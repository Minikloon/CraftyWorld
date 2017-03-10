package world.crafty.pe.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.ResourcePackVersion

enum class ResourcePackClientStatus { UNKNOWN, UNKNOWN_2, REQUEST_INFO, REQUEST_DATA, PLAYER_READY }
class ResourcePackClientResponsePePacket(
        val status: ResourcePackClientStatus,
        val versions: List<ResourcePackVersion>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x09
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ResourcePackClientResponsePePacket) throw IllegalArgumentException()
            stream.writeByte(obj.status.ordinal)
            stream.writeShort(obj.versions.size)
            obj.versions.forEach { ResourcePackVersion.Codec.serialize(it, stream) }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ResourcePackClientResponsePePacket(
                    status = ResourcePackClientStatus.values()[stream.readByte().toInt()],
                    versions = (1..stream.readShort()).map { ResourcePackVersion.Codec.deserialize(stream) }
            )
        }
    }
}