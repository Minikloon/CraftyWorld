package world.crafty.pc.metadata

import world.crafty.common.serialization.MinecraftOutputStream

open class LivingEntityMetadata(
        status: Byte,
        air: Int,
        name: String,
        nameVisible: Boolean,
        silent: Boolean,
        gravity: Boolean,
        val handStatus: Byte,
        val health: Float,
        val potionEffectColor: Int,
        val potionEffectAmbient: Boolean,
        val insertedArrows: Int
) : EntityMetadata(status, air, name, nameVisible, silent, gravity) {
    override fun writeEntriesToStream(stream: MinecraftOutputStream) {
        super.writeEntriesToStream(stream)
        writeEntry(stream, 6, handStatus)
        writeEntry(stream, 7, health)
        writeEntry(stream, 8, potionEffectColor)
        writeEntry(stream, 9, potionEffectAmbient)
        writeEntry(stream, 10, insertedArrows)
    }
}