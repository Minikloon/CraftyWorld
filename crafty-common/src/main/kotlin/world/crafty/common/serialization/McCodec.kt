package world.crafty.common.serialization

import java.io.ByteArrayOutputStream

interface McCodec<T> {
    fun serialized(obj: T) : ByteArray {
        val bs = ByteArrayOutputStream()
        val stream = MinecraftOutputStream(bs)
        serialize(obj, stream)
        return bs.toByteArray()
    }
    fun serialize(obj: T, stream: MinecraftOutputStream)
    fun deserialize(stream: MinecraftInputStream) : T
}