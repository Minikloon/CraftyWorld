package world.crafty.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.compressed
import world.crafty.proto.CraftyChunk
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.CraftyPacket
import world.crafty.proto.MinecraftPlatform
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

enum class ChunkCacheStategy {
    PER_WORLD,
    PER_PLAYER
}

class SetChunkColumnCraftyPacket(
        val chunkColumn: CraftyChunkColumn,
        val hashes: Array<Long>,
        val cacheStrategy: ChunkCacheStategy
) : CraftyPacket() {
    init {
        require(hashes.size == chunkColumn.chunks.size)
    }
    
    constructor(chunkColumn: CraftyChunkColumn, cacheStrategy: ChunkCacheStategy) : this(
            chunkColumn = chunkColumn,
            hashes = chunkColumn.chunks.map { it?.hash ?: CraftyChunk.cachedEmpty.hash }.toTypedArray(),
            cacheStrategy = cacheStrategy
    )
    
    override val codec = Codec
    override val expectedSize = 64000
    
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetChunkColumnCraftyPacket) throw IllegalArgumentException()
            val chunkColumn = obj.chunkColumn
            stream.writeZigzagVarInt(chunkColumn.x)
            stream.writeZigzagVarInt(chunkColumn.x)
            
            val columnBs = ByteArrayOutputStream()
            val columnStream = MinecraftOutputStream(DeflaterOutputStream(columnBs, Deflater(Deflater.BEST_SPEED)))
            chunkColumn.chunks.forEachIndexed { index, chunk -> 
                if(chunk == null) {
                    stream.writeBoolean(false)
                } else {
                    stream.writeBoolean(true)
                    CraftyChunk.Codec.serialize(chunk, columnStream)
                    stream.writeLong(obj.hashes[index])
                }
            }
            columnStream.write(chunkColumn.biomes)
            val compressedColumn = columnBs.toByteArray()
            
            stream.write(compressedColumn)
            stream.writeByte(obj.cacheStrategy.ordinal)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            val chunkX = stream.readZigzagVarInt()
            val chunkZ = stream.readZigzagVarInt()
            
            val compressedColumn = stream.readRemainingBytes()
            val columnStream = MinecraftInputStream(InflaterInputStream(ByteArrayInputStream(compressedColumn)))
            
            val chunksPerColumn = CraftyChunkColumn.chunksPerColumn
            val chunks = arrayOfNulls<CraftyChunk?>(chunksPerColumn)
            val hashes = Array(chunksPerColumn) { CraftyChunk.cachedEmpty.hash }

            for (index in 1..chunksPerColumn) {
                if(stream.readBoolean()) {
                    chunks[index] = CraftyChunk.Codec.deserialize(columnStream)
                    hashes[index] = stream.readLong()
                }
            }
            
            val biomes = stream.readByteArray(256)
            
            val column = CraftyChunkColumn(chunkX, chunkZ, chunks, biomes)
            
            val cacheStrategy = ChunkCacheStategy.values()[stream.readByte().toInt()]
            
            return SetChunkColumnCraftyPacket(column, hashes, cacheStrategy)
        }
    }
}