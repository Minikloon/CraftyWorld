package world.crafty.nbt.tags

import world.crafty.nbt.NbtInputStream
import java.io.DataOutputStream

class NbtString(name: String?, val value: String) : NbtValueTag(name) {
    override val tagType = NbtTagType.STRING
    override val codec = Codec

    override fun deepClone(): NbtTag {
        return NbtString(name, value)
    }

    override fun prettyValueStr(): String {
        return "\"$value\""
    }

    object Codec : NbtTagCodec() {
        override val id = 8

        override fun serialize(obj: Any, stream: DataOutputStream) {
            if(obj !is NbtString) throw IllegalArgumentException()
            stream.writeUTF(obj.value)
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            val value = stream.readUTF()
            return NbtString(name, value)
        }
    }
}