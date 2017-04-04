package world.crafty.pc

import world.crafty.common.Angle256
import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

class PcLocation(
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Angle256 = Angle256.zero,
        val pitch: Angle256 = Angle256.zero
) {
    constructor(loc: Location) : this(loc.x.toDouble(), loc.y.toDouble(), loc.z.toDouble(), loc.yaw, loc.pitch)
    
    fun toLocation() : Location {
        return Location(x.toFloat(), y.toFloat(), z.toFloat(), yaw, pitch)
    }
}

fun MinecraftOutputStream.writePcLocation(loc: PcLocation) {
    writeDouble(loc.x)
    writeDouble(loc.y)
    writeDouble(loc.z)
    writeAngle(loc.yaw)
    writeAngle(loc.pitch)
}

fun MinecraftOutputStream.writePcLocation(loc: Location) {
    writeDouble(loc.x.toDouble())
    writeDouble(loc.y.toDouble())
    writeDouble(loc.z.toDouble())
    writeAngle(loc.yaw)
    writeAngle(loc.pitch)
}

fun MinecraftInputStream.readPcLocation() : PcLocation {
    return PcLocation(
            x = readDouble(),
            y = readDouble(),
            z = readDouble(),
            yaw = readAngle(),
            pitch = readAngle()
    )
}