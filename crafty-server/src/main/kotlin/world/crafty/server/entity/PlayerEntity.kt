package world.crafty.server.entity

import world.crafty.common.Location
import world.crafty.proto.CraftyPacket
import world.crafty.proto.metadata.builtin.*
import world.crafty.proto.packets.server.*
import world.crafty.server.CraftyPlayer
import world.crafty.server.world.World

class PlayerEntity(
        world: World,
        id: Int,
        location: Location,
        val craftyPlayer: CraftyPlayer
) : Entity(world, id, location) {
    val metaEntity = EntityMeta()
    val metaLiving = LivingMeta()
    val metaPlayer = PlayerMeta()
    
    override fun createSpawnPackets(): Collection<CraftyPacket> {
        return listOf(
                AddPlayerCraftyPacket(
                        craftyId = craftyPlayer.id,
                        uuid = craftyPlayer.uuid,
                        username = craftyPlayer.username,
                        entityId = id,
                        location = location,
                        skin = craftyPlayer.skin,
                        meta = metaAll(metaEntity, metaLiving, metaPlayer)
                )
        )
    }

    override fun onSpawn() {
        world.entities.forEach {
            craftyPlayer.send(it.createSpawnPackets())
        }
    }

    override fun createDespawnPackets(): Collection<CraftyPacket> {
        return listOf(
                RemovePlayerCraftyPacket(
                        entityId = id
                )
        )
    }

    override fun getMetaChangesAndClear() = metaChangesAndClear(metaEntity, metaLiving, metaPlayer)
}