package world.crafty.server.world.serialize

import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.server.SetChunkColumnCraftyPacket

interface FullChunkPacketEncoder {
    fun toPacket(chunk: CraftyChunkColumn) : SetChunkColumnCraftyPacket
}

val MinecraftPlatform.chunkPacketEncoder: FullChunkPacketEncoder
    get() = when(this) {
        MinecraftPlatform.PC -> PcFullChunkPacketEncoder
        MinecraftPlatform.PE -> PeFullChunkPacketEncoder
    }