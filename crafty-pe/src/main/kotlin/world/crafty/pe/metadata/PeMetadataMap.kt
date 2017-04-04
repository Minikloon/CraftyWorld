package world.crafty.pe.metadata

import org.joml.Vector3fc
import org.joml.Vector3ic
import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.metadata.MetadataType.*
import world.crafty.pe.proto.PeItem

class PeMetadataMap(private val entries: MutableMap<Int, MetadataEntry> = mutableMapOf()) {
    operator fun set(index: Int, value: Byte) {
        entries[index] = MetadataEntry(BYTE, value)
    }

    operator fun set(index: Int, value: Short) {
        entries[index] = MetadataEntry(SHORT, value)
    }

    operator fun set(index: Int, value: Int) {
        entries[index] = MetadataEntry(INT, value)
    }

    operator fun set(index: Int, value: Float) {
        entries[index] = MetadataEntry(FLOAT, value)
    }

    operator fun set(index: Int, value: String) {
        entries[index] = MetadataEntry(STRING, value)
    }

    operator fun set(index: Int, value: PeItem) {
        entries[index] = MetadataEntry(ITEM_STACK, value.deepClone())
    }

    operator fun set(index: Int, value: Vector3ic) {
        entries[index] = MetadataEntry(INT_COORDINATES, value)
    }

    operator fun set(index: Int, value: Long) {
        entries[index] = MetadataEntry(LONG, value)
    }

    operator fun set(index: Int, value: Vector3fc) {
        entries[index] = MetadataEntry(VECTOR_3, value)
    }

    fun getByte(index: Int) : Byte? {
        val entry = entries[index] ?: throw IllegalStateException("Metadata entry idx $index not found")
        return entry.value as Byte
    }

    object Codec : McCodec<PeMetadataMap> {
        override fun serialize(obj: PeMetadataMap, stream: MinecraftOutputStream) {
            stream.writeSignedVarInt(obj.entries.size)
            obj.entries.forEach { index, entry ->
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