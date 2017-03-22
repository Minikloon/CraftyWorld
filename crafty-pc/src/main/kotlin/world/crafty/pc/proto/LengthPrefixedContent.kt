package world.crafty.pc.proto

import world.crafty.common.serialization.MinecraftOutputStream

interface LengthPrefixedContent {
    fun serializeWithLengthPrefix(stream: MinecraftOutputStream, compressing: Boolean, compressionThreshold: Int)
}