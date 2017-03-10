package world.crafty.nbt

import world.crafty.nbt.tags.NbtTag
import world.crafty.nbt.tags.NbtTagType
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

class NbtInputStream(stream: InputStream) : DataInputStream(stream) {
    constructor(bytes: ByteArray) : this(ByteArrayInputStream(bytes))

    fun readTag() : NbtTag? {
        val id = try {
            readUnsignedByte()
        } catch(ex: EOFException) {
            return null
        }

        val tagFactory = requireNotNull(NbtTagType.idToCodec[id]) { "Unknown id $id while reading nbt" }

        return tagFactory.deserialize(this)
    }
}