package world.crafty.nbt

import world.crafty.common.serialization.LittleEndianDataInputStream
import world.crafty.nbt.tags.NbtTag
import world.crafty.nbt.tags.NbtTagType
import java.io.*

class NbtInputStream(stream: DataInput) : DataInput by stream {
    constructor(bytes: ByteArray, littleEndian: Boolean) : this(
            if(littleEndian)
                LittleEndianDataInputStream(ByteArrayInputStream(bytes))
            else
                DataInputStream(ByteArrayInputStream(bytes))
    )

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