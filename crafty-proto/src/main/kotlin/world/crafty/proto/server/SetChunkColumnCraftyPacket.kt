package world.crafty.proto.server

import world.crafty.proto.CraftyChunk

class SetChunkColumnCraftyPacket(
        val x: Int,
        val z: Int,
        val chunksFromBottom: List<CraftyChunk>,
        val biomes: ByteArray
)