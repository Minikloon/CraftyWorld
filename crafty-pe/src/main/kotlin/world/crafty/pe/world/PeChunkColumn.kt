package world.crafty.pe.world

import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.packets.mixed.CompressionWrapperPePacket
import world.crafty.pe.proto.packets.mixed.EncryptionWrapperPePacket
import world.crafty.pe.proto.packets.server.FullChunkDataPePacket
import world.crafty.proto.CraftyChunkColumn

class PeChunkColumn(
        val x: Int,
        val z: Int,
        val chunks: Array<PeChunk?>,
        val heightMap: ByteArray,
        val biomes: ByteArray
) {
    init {
        require(heightMap.size == 16 * 16 * 2)
        require(biomes.size == 16 * 16)
    }
    
    val expectedSize by lazy {
        9 + chunks.sumBy { it?.byteSize ?: 0 } + heightMap.size + biomes.size
    }

    fun serializeToStreamNoXZ(stream: MinecraftOutputStream) {
        val highestNonAirChunkIndex = chunks.indexOfLast { !(it?.blocks?.all { it == 0.toByte() } ?: true) }
        stream.writeByte(highestNonAirChunkIndex + 1) // number of chunks sent
        (0..highestNonAirChunkIndex)
                .map { chunks[it] }
                .forEach {
                    if(it == null)
                        PeChunk.cachedEmpty.writeToStream(stream)
                    else
                        it.writeToStream(stream)
                }
        stream.write(heightMap)
        stream.write(biomes)
        stream.writeByte(0) // something about border blocks

        stream.writeZigzagVarInt(0) // blocks entities
    }
}

fun CraftyChunkColumn.toPePacket() : EncryptionWrapperPePacket {
    val column = PeChunkColumn(
            x = x,
            z = z,
            chunks = chunks.map { if(it == null) null else PeChunk.convertFromCrafty(it) }.toTypedArray(),
            biomes = biomes,
            heightMap = ByteArray(16 * 16 * 2) // TODO: compute this?
    )
    val packet = FullChunkDataPePacket(column)

    val compressed = CompressionWrapperPePacket(packet)
    return EncryptionWrapperPePacket(compressed.serializedWithId())
}