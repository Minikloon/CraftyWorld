package world.crafty.proto.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class ChunksRadiusResponseCraftyPacket(
        val chunkColumns: List<SetChunkColumnCraftyPacket>
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ChunksRadiusResponseCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.chunkColumns.size)
            obj.chunkColumns.forEach { 
                it.serialize(stream)
            }
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return ChunksRadiusResponseCraftyPacket(
                    chunkColumns = (1..stream.readUnsignedVarInt()).map { SetChunkColumnCraftyPacket.Codec.deserialize(stream) as SetChunkColumnCraftyPacket }
            )
        }
    }
}