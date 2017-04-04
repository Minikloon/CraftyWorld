package world.crafty.pe.metadata

import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector3i
import org.joml.Vector3ic
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PeItem

data class MetadataEntry(
        val type: MetadataType,
        val value: Any
) {
    fun serialize(stream: MinecraftOutputStream) {
        stream.writeUnsignedVarInt(type.ordinal)
        when(type) {
            MetadataType.BYTE -> stream.writeByte(value as Byte)
            MetadataType.SHORT -> stream.writeShort((value as Short).toInt())
            MetadataType.INT -> stream.writeZigzagVarInt(value as Int)
            MetadataType.FLOAT -> stream.writeFloatLe(value as Float)
            MetadataType.STRING -> stream.writeUnsignedString(value as String)
            MetadataType.ITEM_STACK -> {
                val item = value as PeItem
                stream.writeShort(item.id.toInt())
                stream.writeByte(item.count)
                stream.writeShort(item.metadata.toInt())
            }
            MetadataType.INT_COORDINATES -> {
                val vec = value as Vector3ic
                stream.writeSignedVarInt(vec.x())
                stream.writeSignedVarInt(vec.y())
                stream.writeSignedVarInt(vec.z())
            }
            MetadataType.LONG -> stream.writeZigzagVarLong(value as Long)
            MetadataType.VECTOR_3 -> {
                val vec = value as Vector3fc
                stream.writeFloatLe(vec.x())
                stream.writeFloatLe(vec.y())
                stream.writeFloatLe(vec.z())
            }
        }
    }

    companion object {
        fun deserialize(stream: MinecraftInputStream) : MetadataEntry {
            val type = MetadataType.values()[stream.readUnsignedVarInt()]
            val value: Any = when(type) {
                MetadataType.BYTE -> stream.readByte()
                MetadataType.SHORT -> stream.readShort()
                MetadataType.INT -> stream.readZigzagVarInt()
                MetadataType.FLOAT -> stream.readFloatLe()
                MetadataType.STRING -> stream.readUnsignedString()
                MetadataType.ITEM_STACK -> {
                    PeItem(
                            id = stream.readShort(),
                            count = stream.readByte(),
                            metadata = stream.readShort(),
                            extra = null
                    )
                }
                MetadataType.INT_COORDINATES -> {
                    Vector3i(
                            stream.readSignedVarInt(),
                            stream.readSignedVarInt(),
                            stream.readSignedVarInt()
                    )
                }
                MetadataType.LONG -> stream.readZigzagVarLong()
                MetadataType.VECTOR_3 -> {
                    Vector3f(
                            stream.readFloatLe(),
                            stream.readFloatLe(),
                            stream.readFloatLe()
                    )
                }
            }
            return MetadataEntry(type, value)
        }
    }
}