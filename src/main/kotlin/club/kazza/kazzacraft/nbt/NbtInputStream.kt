package club.kazza.kazzacraft.nbt

import club.kazza.kazzacraft.nbt.tags.NbtTag
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

class NbtInputStream(stream: InputStream) : DataInputStream(stream) {
    fun readTag() : NbtTag? {
        val id = try {
            readUnsignedByte()
        } catch(ex: EOFException) {
            return null
        }

        val tagFactory = requireNotNull(tagIdToCodecs[id]) { "Unknown id $id while reading nbt" }

        return tagFactory.deserialize(this)
    }
}