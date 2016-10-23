package club.kazza.kazzacraft.nbt.tags

import club.kazza.kazzacraft.nbt.NbtInputStream
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

        override fun serialize(obj: Any, stream: DataOutputStream) {
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            return NbtEnd()
        }
    }
}