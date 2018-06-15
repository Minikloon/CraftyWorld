package world.crafty.server.entity

import world.crafty.common.Location
import world.crafty.proto.CraftyPacket
import world.crafty.server.world.World
import world.crafty.proto.packets.server.AddEntityCraftyPacket
import world.crafty.proto.packets.server.RemoveEntityCraftyPacket
import world.crafty.proto.metadata.builtin.*

class HorseEntity(
        world: World,
        id: Int,
        location: Location
) : Entity(world, id, location) {
    val metaEntity = EntityMeta()
    val metaLiving = LivingMeta()
    val horseMeta = HorseMeta()

    override fun createSpawnPackets(): Collection<CraftyPacket> {
        return listOf(
                AddEntityCraftyPacket(id, 1, location, metaAll(metaEntity, metaLiving, horseMeta))
        )
    }

    override fun createDespawnPackets(): Collection<CraftyPacket> {
        return listOf(RemoveEntityCraftyPacket(id))
    }

    override fun getMetaChangesAndClear() = metaChangesAndClear(metaEntity, metaLiving, horseMeta)
}