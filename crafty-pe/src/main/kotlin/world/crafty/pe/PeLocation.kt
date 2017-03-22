package world.crafty.pe

import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3f
import org.joml.Vector3fc
import world.crafty.common.Angle256
import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

class PeLocation(
        val x: Float,
        val y: Float,
        val z: Float,
        val yaw: Angle256,
        val pitch: Angle256
) {
    constructor(loc: Location) : this(loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
    
    constructor(pos: Vector3fc, anglesDegree: Vector2fc)
            : this(pos.x(), pos.y(), pos.z(), Angle256.fromDegrees(anglesDegree.x()), Angle256.fromDegrees(anglesDegree.y()))

    fun toLocation() : Location {
        return Location(x, y, z, yaw, pitch)
    }
    
    fun positionVec3() : Vector3f {
        return Vector3f(x, y, z)
    }
    
    fun anglesDegreeVec2() : Vector2f {
        return Vector2f(yaw.toDegrees(), pitch.toDegrees())
    }
}

fun MinecraftOutputStream.writePeLocation(loc: PeLocation) {
    writeFloatLe(loc.x)
    writeFloatLe(loc.y)
    writeFloatLe(loc.z)
    writeAngle(loc.yaw)
    writeAngle(loc.pitch)
}

fun MinecraftInputStream.readPeLocation() : PeLocation {
    return PeLocation(
            x = readFloatLe(),
            y = readFloatLe(),
            z = readFloatLe(),
            yaw = readAngle(),
            pitch = readAngle()
    )
}