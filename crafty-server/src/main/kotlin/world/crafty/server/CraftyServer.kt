package world.crafty.server

import io.vertx.core.AbstractVerticle
import world.crafty.common.utils.logger
import world.crafty.common.vertx.typedSend
import world.crafty.proto.GameMode
import world.crafty.proto.packets.client.JoinRequestCraftyPacket
import world.crafty.proto.registerVertxCraftyCodecs
import world.crafty.proto.metadata.MetaFieldRegistry
import world.crafty.proto.metadata.registerBuiltInMetaDefinitions
import world.crafty.proto.packets.server.JoinResponseCraftyPacket
import world.crafty.proto.packets.server.PreSpawnCraftyPacket
import world.crafty.server.world.World

private val log = logger<CraftyServer>()
class CraftyServer(val address: String, val world: World) : AbstractVerticle() {
    private var playerIdCounter = 0
    private val playersById = mutableMapOf<Int, CraftyPlayer>()
    
    val players : Collection<CraftyPlayer>
        get() = playersById.values
    
    override fun start() {
        val eb = vertx.eventBus()
        registerVertxCraftyCodecs(eb) // TODO: redesign to remove the singletons
        MetaFieldRegistry.registerBuiltInMetaDefinitions()
        
        eb.consumer<JoinRequestCraftyPacket>("$address:join") {
            val request = it.body()
            val playerId = ++playerIdCounter
            val craftyPlayer = CraftyPlayer(this, eb, playerId, request.username, request.authMojang, request.authXbox, request.platform, request.skin, world.spawn)
            playersById[playerId] = craftyPlayer
            log.info { "(Crafty) ${it.body().username} joined, id $playerId!" }
            it.reply(JoinResponseCraftyPacket(playerId, PreSpawnCraftyPacket(
                    entityId = craftyPlayer.entityId,
                    spawnLocation = world.spawn,
                    dimension = 0,
                    gamemode = GameMode.SURVIVAL
            )))
        }
        
        vertx.setPeriodic(50) { world.tick() }
    }
    
    fun typedSendAll(obj: Any) {
        val eb = vertx.eventBus()
        playersById.forEach { id, player -> 
            eb.typedSend("p:c:$id", obj)
        }
    }
    
    fun typedSendAllExcept(except: CraftyPlayer, obj: Any) {
        val eb = vertx.eventBus()
        playersById.forEach { id, player ->
            if(player != except) 
                eb.typedSend("p:c:$id", obj)
        }
    }
}