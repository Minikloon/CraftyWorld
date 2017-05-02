package world.crafty.nbt

import world.crafty.common.serialization.LittleEndianDataInputStream
import world.crafty.nbt.tags.NbtTag
import world.crafty.nbt.tags.NbtTagType
import java.io.*

class NbtInputStream(stream: DataInput) : DataInput by stream {
    constructor(bytes: ByteArray, littleEndian: Boolean)
            : this(pickInputStream(bytes, littleEndian))

    constructor(stream: InputStream) : this(DataInputStream(stream) as DataInput)

    fun readTag() : NbtTag? {
        val id = try {
            readUnsignedByte()
        } catch(ex: EOFException) {
            return null
        }

        val tagFactory = requireNotNull(NbtTagType.idToCodec[id]) { "Unknown id $id while reading nbt" }

        return tagFactory.deserialize(this)
    }

    companion object {
        private fun pickInputStream(bytes: ByteArray, littleEndian: Boolean) : DataInput {
            val bs = ByteArrayInputStream(bytes)
            return if(littleEndian)
                LittleEndianDataInputStream(bs)
            else
                DataInputStream(bs)
        }
    }
}