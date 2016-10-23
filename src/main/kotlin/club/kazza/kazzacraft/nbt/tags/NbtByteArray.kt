package club.kazza.kazzacraft.nbt.tags

import club.kazza.kazzacraft.nbt.NbtInputStream
import java.io.DataOutputStream

class NbtByteArray(name: String?, val value: ByteArray) : NbtValueTag(name) {
    override val tagType = NbtTagType.BYTE_ARRAY
    override val codec = Codec

    override fun deepClone(): NbtTag {
        return NbtByteArray(name, value.clone())
    }

    override fun prettyValueStr(): String {
        return "[${value.size} bytes]"
    }

    object Codec : NbtTagCodec() {
        override val id: Int
            get() = 7

        override fun serialize(obj: Any, stream: DataOutputStream) {
            if(obj !is NbtByteArray) return
            stream.writeInt(obj.value.size)
            stream.write(obj.value)
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            val len = stream.readInt()
            val value = ByteArray(len)
            stream.read(value)
            return NbtByteArray(name, value)
        }
    }
}