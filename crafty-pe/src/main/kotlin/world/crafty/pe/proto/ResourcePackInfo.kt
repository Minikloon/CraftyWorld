package world.crafty.pe.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

class ResourcePackInfo(
        val id: String,
        val version: ResourcePackVersion
) {
    object Codec : PeCodec<ResourcePackInfo> {
        override fun serialize(obj: ResourcePackInfo, stream: MinecraftOutputStream) {
            stream.writeUnsignedString(obj.id)
            ResourcePackVersion.Codec.serialize(obj.version, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): ResourcePackInfo {
            return ResourcePackInfo(
                    id = stream.readUnsignedString(),
                    version = ResourcePackVersion.Codec.deserialize(stream)
            )
        }
    }
}
class ResourcePackVersion(
        val version: String,
        val unknown: Long
) {
    object Codec : PeCodec<ResourcePackVersion> {
        override fun serialize(obj: ResourcePackVersion, stream: MinecraftOutputStream) {
            stream.writeUnsignedString(obj.version)
            stream.writeLong(obj.unknown)
        }
        override fun deserialize(stream: MinecraftInputStream): ResourcePackVersion {
            return ResourcePackVersion(
                    version = stream.readUnsignedString(),
                    unknown = stream.readLong()
            )
        }
    }
}