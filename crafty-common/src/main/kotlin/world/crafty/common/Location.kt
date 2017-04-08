package world.crafty.common

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3ic

class Angle256 constructor(val increment: Byte) {
    fun toDegrees() : Float {
        return increment / 256f * 360
    }
    
    fun toRad() : Float {
        return increment / 256f * (2 * Math.PI.toFloat())
    }
    
    companion object {
        val zero = fromDegrees(0f)
        
        fun fromDegrees(degrees: Float) : Angle256 {
            val increment = degrees / 360 * 256
            return Angle256(increment.toByte())
        }
        
        fun fromRad(rad: Float) : Angle256 {
            val increment = rad / (2 * Math.PI) * 256
            return Angle256(increment.toByte())
        }
    }
}

class Location(
        val x: Float,
        val y: Float,
        val z: Float,
        val yaw: Angle256 = Angle256.zero,
        val pitch: Angle256 = Angle256.zero
) {
    constructor(xyz: Vector3ic) : this(xyz.x().toFloat(), xyz.y().toFloat(), xyz.z().toFloat())

    fun positionVec3() : Vector3f {
        return Vector3f(x, y, z)
    }

    fun anglesDegreeVec2() : Vector2f {
        return Vector2f(yaw.toDegrees(), pitch.toDegrees())
    }
    
    fun add(dx: Float, dy: Float, dz: Float) : Location {
        return Location(x + dx, y + dy, z + dz, yaw, pitch)
    }
    
    fun setY(y: Float) : Location {
        return Location(x, y, z, yaw, pitch)
    }

    override fun toString(): String {
        return "($x, $y, $z; ${yaw.increment}, ${pitch.increment})"
    }
}