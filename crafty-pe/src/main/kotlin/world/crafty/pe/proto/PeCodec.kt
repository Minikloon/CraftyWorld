package world.crafty.pe.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

interface PeCodec<T> {
    fun serialize(obj: T, stream: MinecraftOutputStream)
    fun deserialize(stream: MinecraftInputStream) : T
}