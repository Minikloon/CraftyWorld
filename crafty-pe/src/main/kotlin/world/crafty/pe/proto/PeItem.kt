package world.crafty.pe.proto

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.nbt.tags.NbtCompound
import java.io.ByteArrayInputStream

class PeItem(
        val id: Short,
        val metadata: Short,
        val count: Byte,
        val extra: NbtCompound?
) {
    constructor(id: Int, metadata: Int, count: Int, extra: NbtCompound?)
            : this(id.toShort(), metadata.toShort(), count.toByte(), extra)

    fun deepClone() : PeItem {
        return PeItem(id, metadata, count, extra?.deepClone() as NbtCompound)
    }
    
    object Codec : McCodec<PeItem> {
        fun serializeEmpty(stream: MinecraftOutputStream) {
            stream.writeSignedVarInt(0)
        }
        override fun serialize(obj: PeItem, stream: MinecraftOutputStream) {
            if(obj.id <= 0) {
                serializeEmpty(stream)
                return
            }
            
            stream.writeSignedVarInt(obj.id.toInt())
            stream.writeSignedVarInt((obj.metadata.toInt() shl 8) + obj.count)
            val extraBytes = obj.extra?.toBytes(true)
            if(extraBytes == null)
                stream.writeShort(0)
            else {
                stream.writeShort(extraBytes.size)
                stream.write(extraBytes)
            }
        }
        override fun deserialize(stream: MinecraftInputStream): PeItem {
            val id = stream.readSignedVarInt()
            if(id == 0)
                return PeItem(id.toShort(), 0, 0, null)

            val metadataAndCount = stream.readSignedVarInt()
            val metadata = metadataAndCount ushr 8
            val count = metadataAndCount and 0xFF

            val nbtBytes = stream.readByteArray(stream.readShort().toInt())
            val extra = if(nbtBytes.isEmpty()) null else {
                NbtCompound.deserialize(ByteArrayInputStream(nbtBytes))
            }

            return PeItem(id, metadata, count, extra)
        }
    }
}