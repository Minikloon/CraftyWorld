package world.crafty.server.entity

import world.crafty.proto.CraftyPacket
import world.crafty.proto.metadata.MetaTracker
import world.crafty.proto.metadata.MetaValue
import world.crafty.server.world.World

abstract class Entity(val world: World, val id: Long = world.nextEntityId()) {    
    abstract fun createSpawnPackets() : Collection<CraftyPacket>

    open fun onSpawn() {}
    
    abstract fun createDespawnPackets() : Collection<CraftyPacket>
    
    protected fun metaAll(vararg tracker: MetaTracker) : List<MetaValue> {
        return tracker.flatMap { it.getAllValues() }
    }
    
    abstract fun getMetaChangesAndClear() : List<MetaValue>
    
    protected fun metaChangesAndClear(vararg tracker: MetaTracker) : List<MetaValue> {
        return tracker.flatMap { it.getChangedAndCLear() }
    }
}