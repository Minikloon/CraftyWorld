package world.crafty.nbt.tags

import world.crafty.nbt.NbtInputStream
import java.io.DataOutputStream

class NbtShort(name: String?, val value: Int) : NbtValueTag(name) {
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
            if(obj !is NbtShort) throw IllegalArgumentException()
            stream.writeShort(obj.value)
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            val value = stream.readShort().toInt()
            return NbtShort(name, value)
        }
    }
}