package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.world.PcChunk

class ChunkDataPcPacket(
        val x: Int,
        val z: Int,
        val continuous: Boolean,
        val chunkMask: Int,
        val chunks: Array<PcChunk?>,
        val biomes: ByteArray?
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    override val expectedSize = chunks.sumBy { it?.byteSize ?: 0 }
    companion object Codec : PcPacketCodec() {
        override val id = 0x20
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ChunkDataPcPacket) throw IllegalArgumentException()
            stream.writeInt(obj.x)
            stream.writeInt(obj.z)
            stream.writeBoolean(obj.continuous)
            stream.writeSignedVarInt(obj.chunkMask)
            
            val sections = obj.chunks.filterNotNull()
            val dataSize = sections.sumBy { it.byteSize } + if(obj.biomes == null) 0 else obj.biomes.size
            stream.writeSignedVarInt(dataSize)
            sections.forEach {
                it.writeToStream(stream)
            }
            
            if(obj.biomes != null)
                stream.write(obj.biomes)
            stream.writeSignedVarInt(0)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            throw NotImplementedError()
        }
    }
}