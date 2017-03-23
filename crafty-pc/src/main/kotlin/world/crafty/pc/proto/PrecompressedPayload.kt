package world.crafty.pc.proto

import world.crafty.common.serialization.MinecraftOutputStream

class PrecompressedPayload(val decompressedSize: Int, val payload: ByteArray) : LengthPrefixedContent {
    override fun serializeWithLengthPrefix(stream: MinecraftOutputStream, compressing: Boolean, compressionThreshold: Int) {
        require(compressing) { "Can't send a pre-compressed payload if we're not compressing!" }
        if(decompressedSize != 0) 
            require(decompressedSize >= compressionThreshold) { "Can't send a pre-compressed payload smaller ($decompressedSize) than the threshold ($compressionThreshold)" }
        stream.writeSignedVarInt(MinecraftOutputStream.varIntSize(decompressedSize) + payload.size)
        stream.writeSignedVarInt(decompressedSize)
        stream.write(payload)
    }

    override val expectedSize = payload.size + 8
}