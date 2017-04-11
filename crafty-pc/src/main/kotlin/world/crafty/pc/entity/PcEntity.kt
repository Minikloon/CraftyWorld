package world.crafty.pc.entity

import org.joml.Vector3i
import world.crafty.common.Location
import world.crafty.pc.metadata.PcMetadataMap
import world.crafty.pc.metadata.translators.MetaTranslatorRegistry
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.proto.packets.server.SetEntityHeadLookPcPacket
import world.crafty.pc.proto.packets.server.SetEntityLookPcPacket
import world.crafty.pc.proto.packets.server.SetEntityRelPosAndLookPcPacket
import world.crafty.pc.proto.packets.server.SetEntityRelPosPcPacket
import world.crafty.proto.metadata.MetaValue

open class PcEntity(val id: Int, loc: Location) {
    var loc: Location = loc
        private set
    val metaCache = mutableMapOf<Int, Any>()

    fun metaFromCrafty(translators: MetaTranslatorRegistry, meta: List<MetaValue>) : PcMetadataMap {
        val map = PcMetadataMap()
        meta.forEach {
            val translator = translators[it.fieldId]
                    ?: throw IllegalStateException("No pc meta translator for crafty field ${it.fieldId}")
            val mappings = translator.fromCrafty(this, it)
            mappings?.forEach { fieldId, translated ->
                map[fieldId] = translated
            }
        }
        return map
    }
    
    fun getMovePacketsAndClear(newLoc: Location, onGround: Boolean) : List<PcPacket> {
        if(newLoc == loc)
            return listOf()
        
        val coordsChanged = newLoc.x != loc.x || newLoc.y != loc.y || newLoc.z != loc.z
        val headChanged = newLoc.bodyYaw != loc.bodyYaw || newLoc.headPitch != loc.headPitch
        
        val relMove = Vector3i(
                ((newLoc.x * 32 - loc.x * 32) * 128).toInt(),
                ((newLoc.y * 32 - loc.y * 32) * 128).toInt(),
                ((newLoc.z * 32 - loc.z * 32) * 128).toInt()
        )
        
        val packets = mutableListOf<PcPacket>()
        if(coordsChanged && headChanged) {
            packets.add(SetEntityRelPosAndLookPcPacket(id, relMove.x, relMove.y, relMove.z, newLoc.headYaw, newLoc.headPitch, onGround))
        } else if(headChanged) {
            packets.add(SetEntityLookPcPacket(id, newLoc.headYaw, newLoc.headPitch, onGround))
        } else if(coordsChanged) {
            packets.add(SetEntityRelPosPcPacket(id, relMove.x, relMove.y, relMove.z, onGround))
        }
        
        if(newLoc.headYaw != loc.headYaw) {
            packets.add(SetEntityHeadLookPcPacket(id, newLoc.headYaw))
        }
        
        loc = newLoc
        
        return packets
    }
}