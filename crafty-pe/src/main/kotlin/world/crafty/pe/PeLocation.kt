package world.crafty.pe

import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3f
import org.joml.Vector3fc
import world.crafty.common.Angle256
import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream

data class PeLocation(
        val x: Float,
        val y: Float,
        val z: Float,
        val bodyYaw: Angle256 = Angle256.zero,
        val headYaw: Angle256 = Angle256.zero,
        val headPitch: Angle256 = Angle256.zero
) {
    constructor(loc: Location) : this(loc.x, loc.y, loc.z, loc.bodyYaw, loc.headYaw, loc.headPitch)
    
    constructor(pos: Vector3fc, bodyYaw: Angle256, headYaw: Angle256 = Angle256.zero, pitch: Angle256)
            : this(pos.x(), pos.y(), pos.z(), bodyYaw, headYaw, pitch)

    fun toLocation() : Location {
        return Location(x, y, z, bodyYaw, headYaw, headPitch)
    }
    
    fun positionVec3() : Vector3f {
        return Vector3f(x, y, z)
    }
    
    fun anglesDegreeVec2() : Vector2f {
        return Vector2f(bodyYaw.toDegrees(), headPitch.toDegrees())
    }
}

fun Location.toPe() : PeLocation {
    return PeLocation(this)
}

fun MinecraftOutputStream.writePeLocation(loc: PeLocation) {
    writeFloatLe(loc.x)
    writeFloatLe(loc.y)
    writeFloatLe(loc.z)
    writeAngle(loc.headPitch)
    writeAngle(loc.headYaw)
    writeAngle(loc.bodyYaw)
}

fun MinecraftInputStream.readPeLocation() : PeLocation {
    return PeLocation(
            x = readFloatLe(),
            y = readFloatLe(),
            z = readFloatLe(),
            headPitch = readAngle(),
            headYaw = readAngle(),
            bodyYaw = readAngle()
    )
}

fun MinecraftOutputStream.writePeLocationFloatAngles(loc: PeLocation) {
    writeFloatLe(loc.x)
    writeFloatLe(loc.y)
    writeFloatLe(loc.z)
    writeFloatLe(loc.headPitch.toDegrees())
    writeFloatLe(loc.headYaw.toDegrees())
    writeFloatLe(loc.bodyYaw.toDegrees())
}

fun MinecraftInputStream.readPeLocationFloatAngles() : PeLocation {
    return PeLocation(
            x = readFloatLe(),
            y = readFloatLe(),
            z = readFloatLe(),
            headPitch = Angle256.fromDegrees(readFloatLe()),
            headYaw = Angle256.fromDegrees(readFloatLe()),
            bodyYaw = Angle256.fromDegrees(readFloatLe())
    )
}