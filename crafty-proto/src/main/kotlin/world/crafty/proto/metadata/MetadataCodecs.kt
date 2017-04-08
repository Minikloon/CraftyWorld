package world.crafty.proto.metadata

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

class NullableCodec(val inner: MetaCodec) : MetaCodec {
    override fun serialize(obj: Any?, stream: MinecraftOutputStream) {
        if(obj == null)
            stream.writeBoolean(false)
        else {
            stream.writeBoolean(true)
            inner.serialize(obj, stream)
        }
    }
    override fun deserialize(stream: MinecraftInputStream): Any? {
        val present = stream.readBoolean()
        if(present)
            return inner.deserialize(stream)
        else
            return null
    }
}

object BooleanCodec : MetaCodec {
    override fun serialize(obj: Any?, stream: MinecraftOutputStream) {
        if(obj !is Boolean) throw IllegalArgumentException()
        stream.writeBoolean(obj)
    }
    override fun deserialize(stream: MinecraftInputStream): Any {
        return stream.readBoolean()
    }
}

object IntCodec : MetaCodec {
    override fun serialize(obj: Any?, stream: MinecraftOutputStream) {
        if(obj !is Int) throw IllegalArgumentException()
        stream.writeZigzagVarInt(obj)
    }
    override fun deserialize(stream: MinecraftInputStream): Any {
        return stream.readZigzagVarInt()
    }
}

object FloatCodec : MetaCodec {
    override fun serialize(obj: Any?, stream: MinecraftOutputStream) {
        if(obj !is Float) throw IllegalArgumentException()
        stream.writeFloat(obj)
    }
    override fun deserialize(stream: MinecraftInputStream): Any {
        return stream.readFloat()
    }
}

object StringCodec : MetaCodec {
    override fun serialize(obj: Any?, stream: MinecraftOutputStream) {
        if(obj !is String) throw IllegalArgumentException()
        stream.writeUnsignedString(obj)
    }
    override fun deserialize(stream: MinecraftInputStream): Any? {
        return stream.readUnsignedString()
    }
}