package world.crafty.pe.entity

import world.crafty.pe.PeLocation
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.packets.mixed.MoveMode
import world.crafty.pe.proto.packets.mixed.SetPlayerLocPePacket

class PePlayerEntity(
        id: Long
) : PeEntity(id) {
    override fun getSetLocationPacket(loc: PeLocation, onGround: Boolean): PePacket {
        return SetPlayerLocPePacket(id, loc.copy(y = loc.y + eyeHeight), MoveMode.INTERPOLATE, onGround)
    }
    
    companion object {
        val height = 1.8f
        val eyeHeight = 1.62f
    }
}