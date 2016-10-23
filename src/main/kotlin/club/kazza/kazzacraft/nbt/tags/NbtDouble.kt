package club.kazza.kazzacraft.nbt.tags

import club.kazza.kazzacraft.nbt.NbtInputStream
import java.io.DataOutputStream

class NbtDouble(name: String?, val value: Double) : NbtValueTag(name) {
    override val tagType = NbtTagType.DOUBLE
    override val codec = Codec

    override fun deepClone(): NbtTag {
        return NbtDouble(name, value)
    }

    override fun prettyValueStr(): String {
        return value.toString()
    }

    object Codec : NbtTagCodec() {
        override val id = 6

        override fun serialize(obj: Any, stream: DataOutputStream) {
            if(obj !is NbtDouble) throw IllegalArgumentException()
            stream.writeDouble(obj.value)
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            val value = stream.readDouble()
            return NbtDouble(name, value)
        }
    }
}