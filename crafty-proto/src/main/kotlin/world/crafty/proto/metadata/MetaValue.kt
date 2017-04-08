package world.crafty.proto.metadata

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

class MetaValue(
        val fieldId: Int,
        val value: Any?
) {
    companion object {
        fun serialize(values: Collection<MetaValue>, stream: MinecraftOutputStream) {
            checkRegistryEmpty()
            stream.writeUnsignedVarInt(values.size)
            values.forEach { meta ->
                val field = MetaFieldRegistry[meta.fieldId] ?: throw IllegalStateException("Unknown pc meta field id ${meta.fieldId}")
                stream.writeUnsignedVarInt(field.id)
                field.codec.serialize(meta.value, stream)
            }
        }
        
        fun deserialize(stream: MinecraftInputStream) : List<MetaValue> {
            checkRegistryEmpty()
            val count = stream.readUnsignedVarInt()
            return (1..count).map {
                val fieldId = stream.readUnsignedVarInt()
                val field = MetaFieldRegistry[fieldId] ?: throw IllegalStateException("Unknown pc meta field id $fieldId")
                val value = field.codec.deserialize(stream)
                MetaValue(fieldId, value)
            }
        }
        
        private fun checkRegistryEmpty() {
            if(MetaFieldRegistry.size == 0)
                throw IllegalStateException("MetaFieldRegistry is empty! Did you forget to register the built-in definitions?")
        }
    }
}

object MetaFieldRegistry {
    private val fields = mutableMapOf<Int, MetaField>()

    operator fun get(fieldId: Int) : MetaField? {
        return fields[fieldId]
    }

    val size: Int
        get() = fields.size

    fun registerField(field: MetaField) {
        val previous = fields.put(field.id, field)
        if(previous != null)
            throw IllegalStateException("Dupe crafty meta field registration id ${field.id} between ${previous.name} and ${field.name}")
    }
}