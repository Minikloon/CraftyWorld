package world.crafty.server

import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import kotlinx.coroutines.experimental.launch
import world.crafty.common.Location
import world.crafty.common.utils.logger
import world.crafty.common.utils.sinceThen
import world.crafty.common.vertx.CurrentVertx
import world.crafty.common.vertx.typedConsumer
import world.crafty.common.vertx.typedSend
import world.crafty.proto.CraftyPacket
import world.crafty.proto.CraftySkin
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.packets.client.*
import world.crafty.proto.packets.server.*
import world.crafty.server.entity.PlayerEntity
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

private val log = logger<CraftyPlayer>()
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
    
    private val pings = mutableMapOf<Int, CraftyPing>()
    private var pingCounter = 0

    private val timerIds = mutableSetOf<Long>() // TODO: make player a verticle to avoid this shit?
    private val consumers = mutableSetOf<MessageConsumer<*>>()
    
    init {
        setPeriodic(1000) {
            val ping = CraftyPing(pingCounter++, Instant.now())
            pings[ping.id] = ping
            val pingPacket = PingCraftyPacket(ping.id)
            send(pingPacket)
            
            val oldestPing = pings.values.maxBy { it.timestamp.sinceThen() } ?: return@setPeriodic
            if(oldestPing.timestamp.sinceThen() > Duration.ofMillis(2000)) {
                server.disconnectPlayer(id, "Timeout")
            }
        }
        
        consume(PongCraftyPacket::class) {
            pings.remove(it.id)
        }
        
        consume(ChatFromClientCraftyPacket::class) {
            val text = "$username > ${it.text}"
            server.players.forEach {
                it.send(ChatMessageCraftyPacket(text))
            }
        }

        consumeMessage(ChunksRadiusRequestCraftyPacket::class) {
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
            entity = world.spawn { world, id -> PlayerEntity(world, entityId, spawnLocation, this) }
            world.addViewer(this)
            log.info { "Crafty spawning player $username!" }
        }
        
        consume(SetPlayerPosCraftyPacket::class) {
            val coords = it.coords
            val prevLoc = entity?.location
            val newLoc = entity?.location?.copy(x = coords.x(), y = coords.y(), z = coords.z())
            if(prevLoc != null && newLoc != null)
                entity?.location = newLoc
            entity?.onGround = it.onGround
        }
        
        consume(SetPlayerLookCraftyPacket::class) {
            val prevLoc = entity?.location
            val newLoc = entity?.location?.copy(bodyYaw = it.bodyYaw, headYaw = it.headYaw, headPitch = it.headPitch)
            if(prevLoc != null && newLoc != null)
                entity?.location = newLoc
        }
        
        consume(SetPlayerPosAndLookCraftyPacket::class) {
            entity?.location = it.loc
            entity?.onGround = it.onGround
        }

        consume(PlayerActionCraftyPacket::class) {
            val action = it.action
            when(it.action) {
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
    
    fun <T: Any> consume(clazz: KClass<T>, onReceive: (packet: T) -> Unit) {
        consumeMessage(clazz) { onReceive(it.body()) }
    }
    
    fun <T: Any> consumeMessage(clazz: KClass<T>, onReceive: (packet: Message<T>) -> Unit) {
        val consumer = eb.typedConsumer("p:s:$id", clazz, onReceive)
        consumers.add(consumer)
    }
    
    fun send(packets: Collection<CraftyPacket>) {
        packets.forEach { send(it) }
    }
    
    fun send(packet: CraftyPacket) {
        try {
            eb.typedSend("p:c:$id", packet)
        } catch(e: Exception) {
            log.debug { "Error sending packet of type ${packet::class.simpleName} to player $username" }
            server.disconnectPlayer(id)
        }
    }

    fun setPeriodic(millis: Long, action: suspend () -> Unit) {
        val timerId = server.vertx.setPeriodic(millis) {
            launch(CurrentVertx) {
                action()
            }
        }
        timerIds.add(timerId)
    }
    
    fun onDisconnect() {
        server.world.removeViewer(this)
        entity?.despawn()
        consumers.forEach { it.unregister() }
    }
}

private data class CraftyPing(
        val id: Int,
        val timestamp: Instant
)