package world.crafty.nbt.tags

import world.crafty.nbt.NbtInputStream
import java.io.DataOutput
import java.io.DataOutputStream

class NbtEnd : NbtTag(null) {
    override val tagType = NbtTagType.END
    override val hasValue = false
    override val codec = Codec

    override fun deepClone(): NbtTag {
        return NbtEnd()
    }

    override fun prettyPrint(sb: StringBuilder, indentStr: String, indentLevel: Int) {
    }

    object Codec : NbtTagCodec() {
        override val id = 0

        override fun serialize(obj: Any, stream: DataOutput) {
        }
        override fun deserialize(stream: NbtInputStream): NbtTag {
            return deserialize(null, stream)
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            return NbtEnd()
        }
    }
}