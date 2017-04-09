package world.crafty.server

import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import world.crafty.common.Location
import world.crafty.common.utils.getLogger
import world.crafty.common.utils.info
import world.crafty.common.vertx.typedConsumer
import world.crafty.common.vertx.typedSend
import world.crafty.proto.CraftyPacket
import world.crafty.proto.CraftySkin
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.packets.client.*
import world.crafty.proto.packets.server.*
import world.crafty.server.entity.PlayerEntity
import java.util.*
import kotlin.reflect.KClass

private val log = getLogger<CraftyPlayer>()
class CraftyPlayer(
        private val server: CraftyServer,
        private val eb: EventBus,
        val id: Int,
        val username: String,
        val authMojang: Boolean,
        val authXbox: Boolean,
        val platform: MinecraftPlatform,
        val skin: CraftySkin,
        val spawnLocation: Location
) {
    val uuid: UUID = UUID.randomUUID()
    val entityId = server.world.nextEntityId()
    var entity: PlayerEntity? = null // TODO: split this class into spawned/unspawned to avoid null checks
    
    init {
        consume(ChatFromClientCraftyPacket::class) {
            val body = it.body()
            val text = "$username > ${body.text}"
            server.players.forEach {
                eb.send("p:c:${it.id}:chat", ChatMessageCraftyPacket(text))
            }
        }

        consume(ChunksRadiusRequestCraftyPacket::class) {
            val body = it.body()
            val radius = body.radius
            val pos = body.pos
            val world = server.world

            val columns = world.chunks
                    .filter { (it.x - pos.x() / 16) * (it.x - pos.x() / 16) + (it.z - pos.z() / 16) * (it.z - pos.z() / 16) < radius * radius }
                    .map { it.toPacket() }

            it.reply(ChunksRadiusResponseCraftyPacket(columns))
        }

        consume(ReadyToSpawnCraftyPacket::class) {
            val world = server.world
            eb.typedSend("p:c:$id", SpawnSelfCraftyPacket())
            entity = world.spawn { PlayerEntity(it, entityId, this, spawnLocation) }
            world.addViewer(this)
            log.info { "Crafty spawning player $username!" }
        }
        
        consume(PlayerActionCraftyPacket::class) {
            val action = it.body().action
            when(it.body().action) {
                PlayerAction.START_SNEAK -> {
                    entity?.metaPlayer?.crouched = true
                }
                PlayerAction.STOP_SNEAK -> {
                    entity?.metaPlayer?.crouched = false
                }
                else -> {
                    log.info { "Crafty Unimplemented action $action" }
                }
            }
        }
    }
    
    fun <T: Any> consume(clazz: KClass<T>, onReceive: (packet: Message<T>) -> Unit) {
        eb.typedConsumer("p:s:$id", clazz, onReceive)
    }
    
    fun send(packets: Collection<CraftyPacket>) {
        packets.forEach { send(it) }
    }
    
    fun send(packet: CraftyPacket) {
        eb.typedSend("p:c:$id", packet)
    }
}