package world.crafty.proto

import world.crafty.proto.packets.server.SetChunkColumnCraftyPacket
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class ConcurrentColumnsCache<TChunkColumnPacket> {
    private val columns = mutableMapOf<Long, CachedColumn<TChunkColumnPacket>>()
    private val rwLock = ReentrantReadWriteLock()
    
    fun getOrComputeChunkPacket(setChunk: SetChunkColumnCraftyPacket, craftyToPlatform: (CraftyChunkColumn) -> TChunkColumnPacket) : TChunkColumnPacket {
        val craftyChunk = setChunk.chunkColumn
        val index = getIndex(craftyChunk.x, craftyChunk.z)
        val columnHash = setChunk.hashes.reduce { acc, chunkHash -> acc xor chunkHash }

        rwLock.read {
            val cached = columns[index]
            if(cached != null && cached.hash == columnHash)
                return cached.packet
        }
        
        rwLock.write {
            val cached = columns[index]
            if(cached != null && cached.hash == columnHash) cached.packet
            val computed = CachedColumn(
                    packet = craftyToPlatform(craftyChunk),
                    hash = columnHash
            )
            columns[index] = computed
            return computed.packet
        }
    }
    private fun getIndex(chunkX: Int, chunkZ: Int) : Long {
        return (chunkX.toLong() shl 32) or chunkZ.toLong()
    }
    
    private class CachedColumn<out TPacket>(
            val packet: TPacket,
            val hash: Long
    )
}