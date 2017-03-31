package world.crafty.pe.proto

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.nbt.tags.NbtCompound

class PeItem(
        val id: Short,
        val metadata: Short,
        val count: Byte,
        val extra: NbtCompound?
) {
    
    object Codec : McCodec<PeItem> {
        fun serializeEmpty(stream: MinecraftOutputStream) {
            stream.writeSignedVarInt(0)
        }
        override fun serialize(obj: PeItem, stream: MinecraftOutputStream) {
            if(obj.id < 0) {
                serializeEmpty(stream)
                return
            }
            
            stream.writeSignedVarInt(obj.id.toInt())
            stream.writeSignedVarInt((obj.metadata.toInt() shl 8) + obj.count)
            
            
        }
        override fun deserialize(stream: MinecraftInputStream): PeItem {
        }
    }
}