package world.crafty.pe.metadata

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

class PeMetadataMap(private val entriesByFieldIndex: MutableMap<Int, MetadataEntry> = mutableMapOf()) {
    operator fun set(index: Int, entry: MetadataEntry) {
        entriesByFieldIndex[index] = entry
    }
    
    val entries: Collection<MetadataEntry>
        get() = entriesByFieldIndex.values
    
    object Codec : McCodec<PeMetadataMap> {
        override fun serialize(obj: PeMetadataMap, stream: MinecraftOutputStream) {
            stream.writeSignedVarInt(obj.entriesByFieldIndex.size)
            obj.entriesByFieldIndex.forEach { index, entry ->
                stream.writeUnsignedVarInt(index)
                entry.serialize(stream)
            }
        }
        override fun deserialize(stream: MinecraftInputStream): PeMetadataMap {
            val count = stream.readSignedVarInt()

            val entries = mutableMapOf<Int, MetadataEntry>()
            repeat(count) {
                val index = stream.readUnsignedVarInt()
                val entry = MetadataEntry.deserialize(stream)
                entries[index] = entry
            }
            return PeMetadataMap(entries)
        }
    }
}