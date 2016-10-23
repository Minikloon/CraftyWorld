package club.kazza.kazzacraft.nbt.tags

import club.kazza.kazzacraft.nbt.NbtInputStream
import java.io.DataOutputStream

class NbtFloat(name: String?, val value: Float) : NbtValueTag(name) {
    override val tagType = NbtTagType.FLOAT
    override val codec = Codec

    override fun deepClone(): NbtTag {
        return NbtFloat(name, value)
    }

    override fun prettyValueStr(): String {
        return value.toString()
    }

    object Codec : NbtTagCodec() {
        override val id = 5

        override fun serialize(obj: Any, stream: DataOutputStream) {
            if(obj !is NbtFloat) return
            stream.writeFloat(obj.value)
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            val value = stream.readFloat()
            return NbtFloat(name, value)
        }
    }
}