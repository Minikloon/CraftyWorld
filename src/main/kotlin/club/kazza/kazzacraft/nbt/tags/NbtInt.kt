package club.kazza.kazzacraft.nbt.tags

import club.kazza.kazzacraft.nbt.NbtInputStream
import java.io.DataOutputStream

class NbtInt(name: String?, val value: Int) : NbtValueTag(name) {
    override val tagType = NbtTagType.INT
    override val codec = Codec

    override fun deepClone(): NbtTag {
        return NbtInt(name, value)
    }

    override fun prettyValueStr() : String {
        return value.toString()
    }

    object Codec : NbtTagCodec() {
        override val id = 3

        override fun serialize(obj: Any, stream: DataOutputStream) {
            if(obj !is NbtInt) return
            stream.writeInt(obj.value)
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            val value = stream.readInt()
            return NbtInt(name, value)
        }
    }
}