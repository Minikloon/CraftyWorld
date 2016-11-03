package club.kazza.kazzacraft.metadata

import club.kazza.kazzacraft.network.protocol.McChat
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream

open class EntityMetadata(
        val status: Byte,
        val air: Int,
        val name: String,
        val nameVisible: Boolean,
        val silent: Boolean,
        val gravity: Boolean
) {
    fun writeToStream(stream: MinecraftOutputStream) {
        writeEntriesToStream(stream)
        writeEntry(stream, 0xFF, null)
    }

    open fun writeEntriesToStream(stream: MinecraftOutputStream) {
        writeEntry(stream, 0, status)
        writeEntry(stream, 1, air)
        writeEntry(stream, 2, name)
        writeEntry(stream, 3, nameVisible)
        writeEntry(stream, 4, silent)
        writeEntry(stream, 5, gravity)
    }

    protected fun writeEntry(stream: MinecraftOutputStream, index: Int, value: Any?) {
        stream.writeByte(index)
        when(value) {
            is Byte -> {
                stream.writeByte(0)
                stream.writeByte(value)
            }
            is Int -> {
                stream.writeByte(1)
                stream.writeVarInt(value)
            }
            is Float -> {
                stream.writeByte(2)
                stream.writeFloat(value)
            }
            is String -> {
                stream.writeByte(3)
                stream.writeString(value)
            }
            is McChat -> {
                stream.writeByte(4)
                stream.writeJson(value)
            }
            // TODO: Slot
            is Boolean -> {
                stream.writeByte(6)
                stream.writeBoolean(value)
            }
            // TODO: Rotation
            // TODO: Position
            // TODO: OptPosition
            // TODO: Direction
            // TODO: OptUUID
            // TODO: OptBlocKID
        }
    }
}