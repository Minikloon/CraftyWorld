package world.crafty.pc.metadata

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import java.util.*

class PcMetadataMap(private val entries: MutableMap<Int, MetadataEntry> = mutableMapOf()) {
    operator fun set(index: Int, value: Byte) {
        entries[index] = MetadataEntry(MetadataType.BYTE, value)
    }

    operator fun set(index: Int, value: Int) {
        entries[index] = MetadataEntry(MetadataType.VARINT, value)
    }

    operator fun set(index: Int, value: Float) {
        entries[index] = MetadataEntry(MetadataType.FLOAT, value)
    }

    operator fun set(index: Int, value: String) {
        entries[index] = MetadataEntry(MetadataType.STRING, value)
    }

    operator fun set(index: Int, value: Boolean) {
        entries[index] = MetadataEntry(MetadataType.BOOLEAN, value)
    }

    operator fun set(index: Int, value: UUID?) {
        entries[index] = MetadataEntry(MetadataType.OPTIONAL_UUID, value)
    }
    
    object Codec : McCodec<PcMetadataMap> {
        override fun serialize(obj: PcMetadataMap, stream: MinecraftOutputStream) {
            obj.entries.forEach { index, entry -> 
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
}