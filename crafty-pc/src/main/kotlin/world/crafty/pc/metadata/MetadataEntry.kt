package world.crafty.pc.metadata

import org.joml.Vector3fc
import org.joml.Vector3ic
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import java.util.*

data class MetadataEntry(
        val type: MetadataType,
        val value: Any?
) {
    init {
        require(when(type) {
            MetadataType.BYTE -> value is Byte
            MetadataType.VARINT -> value is Int
            MetadataType.FLOAT -> value is Float
            MetadataType.STRING -> value is String
            MetadataType.CHAT -> throw NotImplementedError()
            MetadataType.ITEM_STACK -> throw NotImplementedError()
            MetadataType.BOOLEAN -> value is Boolean
            MetadataType.ROTATION -> value is Vector3fc
            MetadataType.POSITION -> value is Vector3ic
            MetadataType.OPTIONAL_POSITION -> value is Vector3ic? 
            MetadataType.DIRECTION -> value is Int
            MetadataType.OPTIONAL_UUID -> value is UUID?
            MetadataType.OPTIONAL_BLOCK_ID -> throw NotImplementedError()
        }) {
            if(value == null)
                throw IllegalArgumentException("metadata entry value is null for type $type")
            else
                throw IllegalArgumentException("metadata entry value is ${value::class.qualifiedName} for type $type")
        }
    }
    
    fun serialize(stream: MinecraftOutputStream) {
        stream.writeUnsignedVarInt(type.ordinal)
        when(type) {
            MetadataType.BYTE -> stream.writeByte(value as Byte)
            MetadataType.VARINT -> stream.writeSignedVarInt(value as Int)
            MetadataType.FLOAT -> stream.writeFloat(value as Float)
            MetadataType.STRING -> stream.writeSignedString(value as String)
            MetadataType.CHAT -> throw NotImplementedError()
            MetadataType.ITEM_STACK -> throw NotImplementedError()
            MetadataType.BOOLEAN -> stream.writeBoolean(value as Boolean)
            MetadataType.ROTATION -> stream.writeVector3f(value as Vector3fc)
            MetadataType.POSITION -> stream.writeBlockLocation(value as Vector3ic)
            MetadataType.OPTIONAL_POSITION -> {
                val pos = value as Vector3ic?
                if(pos == null)
                    stream.writeBoolean(false)
                else {
                    stream.writeBoolean(true)
                    stream.writeBlockLocation(pos)
                }
            }
            MetadataType.DIRECTION -> stream.writeSignedVarInt(value as Int)
            MetadataType.OPTIONAL_UUID -> {
                val uuid = value as UUID?
                if(uuid == null)
                    stream.writeBoolean(false)
                else {
                    stream.writeBoolean(true)
                    stream.writeUuid(uuid)
                }
            }
            MetadataType.OPTIONAL_BLOCK_ID -> throw NotImplementedError()
        }
    }

    companion object {
        fun deserialize(stream: MinecraftInputStream) : MetadataEntry {
            val type = MetadataType.values()[stream.readUnsignedVarInt()]
            val value: Any? = when(type) {
                MetadataType.BYTE -> stream.readByte()
                MetadataType.VARINT -> stream.readSignedVarInt()
                MetadataType.FLOAT -> stream.readFloat()
                MetadataType.STRING -> stream.readSignedString()
                MetadataType.CHAT -> throw NotImplementedError()
                MetadataType.ITEM_STACK -> throw NotImplementedError()
                MetadataType.BOOLEAN -> stream.readBoolean()
                MetadataType.ROTATION -> stream.readVector3f()
                MetadataType.POSITION -> stream.readBlockLocation()
                MetadataType.OPTIONAL_POSITION -> {
                    val hasValue = stream.readBoolean()
                    if(hasValue)
                        stream.readBlockLocation()
                    else null
                }
                MetadataType.DIRECTION -> stream.readSignedVarInt()
                MetadataType.OPTIONAL_UUID -> {
                    val hasValue = stream.readBoolean()
                    if(hasValue)
                        stream.readUuid()
                    else null
                }
                MetadataType.OPTIONAL_BLOCK_ID -> throw NotImplementedError()
            }
            return MetadataEntry(type, value)
        }
    }
}