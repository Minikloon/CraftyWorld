package world.crafty.pc.world

import org.joml.Vector3ic

data class Location(
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float = 0f,
        val pitch: Float = 0f
) {
    constructor(xyz: Vector3ic) : this(xyz.x().toDouble(), xyz.y().toDouble(), xyz.z().toDouble())
}