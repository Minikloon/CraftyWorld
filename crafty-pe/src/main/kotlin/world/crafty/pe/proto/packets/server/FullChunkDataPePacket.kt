package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.world.PeChunkColumn

class FullChunkDataPePacket(
        val column: PeChunkColumn
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    override val expectedSize = column.expectedSize + 12
    object Codec : PePacketCodec() {
        override val id = 0x3a
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is FullChunkDataPePacket) throw IllegalArgumentException()
            val column = obj.column
            val encoded = MinecraftOutputStream.serialized(column.expectedSize) { stream -> 
                column.serializeToStreamNoXZ(stream)
            }
            
            stream.writeZigzagVarInt(column.x)
            stream.writeZigzagVarInt(column.z)
            stream.writeUnsignedVarInt(encoded.size)
            stream.write(encoded)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            throw NotImplementedError()
        }
    }
}