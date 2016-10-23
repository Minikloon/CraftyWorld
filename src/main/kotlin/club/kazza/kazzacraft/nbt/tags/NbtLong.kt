package club.kazza.kazzacraft.nbt.tags

import club.kazza.kazzacraft.nbt.NbtInputStream
import java.io.DataOutputStream

class NbtLong(name: String?, val value: Long) : NbtValueTag(name) {
    override val tagType = NbtTagType.LONG
    override val codec = Codec

    override fun deepClone(): NbtTag {
        return NbtLong(name, value)
    }

    override fun prettyValueStr(): String {
        return value.toString()
    }

    object Codec : NbtTagCodec() {
        override val id = 4

        override fun serialize(obj: Any, stream: DataOutputStream) {
            if(obj !is NbtLong) return
            stream.writeLong(obj.value)
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            val value = stream.readLong()
            return NbtLong(name, value)
        }
    }
}