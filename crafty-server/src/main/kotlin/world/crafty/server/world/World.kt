package world.crafty.server.world

import world.crafty.common.Location
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.CraftyPacket
import world.crafty.proto.packets.server.PatchEntityCraftyPacket
import world.crafty.server.CraftyPlayer
import world.crafty.server.entity.Entity
import world.crafty.server.utils.broadcast

class World(val chunks: List<CraftyChunkColumn>, val spawn: Location) {
    private val viewers = mutableSetOf<CraftyPlayer>()
    
    private val entitiesById = mutableMapOf<Int, Entity>()
    val entities: Collection<Entity>
        get() = entitiesById.values

    private var entityIdCounter = 0
    fun nextEntityId() : Int {
        return ++entityIdCounter
    }
    
    fun <T: Entity> spawn(spawner: (world: World) -> T) : T {
        val id = ++entityIdCounter
        val entity = spawner(this)
        
        entity.createSpawnPackets().broadcast(viewers)
        entity.onSpawn()

        entitiesById[id] = entity
        
        return entity
    }
    
    fun tick() {
        if(viewers.size == 0)
            return
        
        entities.forEach {
            val pos = it.getPositionPacketIfMovedAndReset()
            if(pos != null)
                sendAllViewers(pos)
            
            val changes = it.getMetaChangesAndClear()
            if(!changes.isEmpty()) {
                val patch = PatchEntityCraftyPacket(it.id, changes)
                sendAllViewers(patch)
            }
        }
    }
    
    fun addViewer(player: CraftyPlayer) {
        viewers.add(player)
    }
    
    fun removeViewer(player: CraftyPlayer) {
        viewers.remove(player)
    }
    
    fun sendAllViewers(packet: CraftyPacket) {
        viewers.forEach {
            it.send(packet)
        }
    }
    
    fun getEntity(id: Int) : Entity? {
        return entitiesById[id]
    }
}