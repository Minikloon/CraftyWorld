package world.crafty.pc.metadata

import world.crafty.common.serialization.MinecraftOutputStream

class PlayerMetadata(
        status: Byte,
        air: Int,
        name: String,
        nameVisible: Boolean,
        silent: Boolean,
        gravity: Boolean,
        handStatus: Byte,
        health: Float,
        potionEffectColor: Int,
        potionEffectAmbient: Boolean,
        insertedArrows: Int,
        val extraHearts: Int,
        val score: Int,
        val skinParts: Byte,
        val mainHand: Byte
) : LivingEntityMetadata(status, air, name, nameVisible, silent, gravity, handStatus, health, potionEffectColor, potionEffectAmbient, insertedArrows) {
    override fun writeEntriesToStream(stream: MinecraftOutputStream) {
        super.writeEntriesToStream(stream)
        writeEntry(stream, 11, extraHearts)
        writeEntry(stream, 12, score)
        writeEntry(stream, 13, skinParts)
        writeEntry(stream, 14, mainHand)
    }
}