package world.crafty.pc.metadata

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.metadata.MetaValue
import java.util.*

class PcMetadataMap(private val entriesByFieldIndex: MutableMap<Int, MetadataEntry> = mutableMapOf()) {
    operator fun set(index: Int, entry: MetadataEntry) {
        entriesByFieldIndex[index] = entry
    }
    
    val entries: Collection<MetadataEntry>
        get() = entriesByFieldIndex.values
    
    object Codec : McCodec<PcMetadataMap> {
        override fun serialize(obj: PcMetadataMap, stream: MinecraftOutputStream) {
            obj.entriesByFieldIndex.forEach { index, entry -> 
                stream.writeByte(index)
                entry.serialize(stream)
            }
            stream.writeByte(0xff)
        }
        override fun deserialize(stream: MinecraftInputStream): PcMetadataMap {
            val entries = mutableMapOf<Int, MetadataEntry>()
            while(true) {
                val index = stream.readUnsignedByte()
                if(index == 255) break
                entries[index] = MetadataEntry.deserialize(stream)
            }
            return PcMetadataMap(entries)
        }
    }
    
    companion object {
        fun fromCrafty(entityId: Int, values: List<MetaValue>) : PcMetadataMap {
            val entries = mutableMapOf<Int, MetadataEntry>()
            
            return PcMetadataMap(entries)
        }
    }
}