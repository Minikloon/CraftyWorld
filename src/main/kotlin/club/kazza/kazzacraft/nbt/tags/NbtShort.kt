package club.kazza.kazzacraft.nbt.tags

import club.kazza.kazzacraft.nbt.NbtInputStream
import java.io.DataOutputStream

class NbtShort(name: String?, val value: Short) : NbtValueTag(name) {
    override val tagType = NbtTagType.SHORT
    override val codec = Codec

    override fun deepClone(): NbtTag {
        return NbtShort(name, value)
    }

    override fun prettyValueStr(): String {
        return value.toString()
    }

    object Codec : NbtTagCodec() {
        override val id = 2

        override fun serialize(obj: Any, stream: DataOutputStream) {
            if(obj !is NbtShort) return
            stream.writeShort(obj.value)
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
        }

    }
}