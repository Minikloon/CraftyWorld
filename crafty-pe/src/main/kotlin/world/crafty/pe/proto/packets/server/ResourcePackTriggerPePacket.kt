package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.ResourcePackInfo

class ResourcePackTriggerPePacket(
        val mustAccept: Boolean,
        val behaviors: List<ResourcePackInfo>,
        val resources: List<ResourcePackInfo>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x07
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ResourcePackTriggerPePacket) throw IllegalArgumentException()
            stream.writeBoolean(obj.mustAccept)
            stream.writeShort(obj.behaviors.size)
            obj.behaviors.forEach { ResourcePackInfo.Codec.serialize(it, stream) }
            stream.writeShort(obj.resources.size)
            obj.resources.forEach { ResourcePackInfo.Codec.serialize(it, stream) }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ResourcePackTriggerPePacket(
                    mustAccept = stream.readBoolean(),
                    behaviors = (1..stream.readShort()).map { ResourcePackInfo.Codec.deserialize(stream) },
                    resources = (1..stream.readShort()).map { ResourcePackInfo.Codec.deserialize(stream) }
            )
        }
    }
}