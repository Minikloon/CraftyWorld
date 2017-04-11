package world.crafty.pc

import world.crafty.common.Angle256
import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

fun MinecraftOutputStream.writePcLocation(loc: Location) {
    writeDouble(loc.x.toDouble())
    writeDouble(loc.y.toDouble())
    writeDouble(loc.z.toDouble())
    writeAngle(loc.bodyYaw)
    writeAngle(loc.headPitch)
}

fun MinecraftInputStream.readPcLocation() : Location {
    return Location(
            x = readDouble().toFloat(),
            y = readDouble().toFloat(),
            z = readDouble().toFloat(),
            bodyYaw = readAngle(),
            headPitch = readAngle()
    )
}

fun MinecraftOutputStream.writePcLocationFloatAngles(loc: Location) {
    writeDouble(loc.x.toDouble())
    writeDouble(loc.y.toDouble())
    writeDouble(loc.z.toDouble())
    writeFloat(loc.bodyYaw.toDegrees())
    writeFloat(loc.headPitch.toDegrees())
}

fun MinecraftInputStream.readPcLocationFloatAngles() : Location {
    val x = readDouble().toFloat()
    val y = readDouble().toFloat()
    val z = readDouble().toFloat()
    val bodyYaw = Angle256.fromDegrees(readFloat())
    return Location(
            x = x,
            y = y,
            z = z,
            bodyYaw = bodyYaw,
            headYaw = bodyYaw.copy(),
            headPitch = Angle256.fromDegrees(readFloat())
    )
}