package world.crafty.server

import io.vertx.core.AbstractVerticle
import world.crafty.common.vertx.typedSend
import world.crafty.proto.GameMode
import world.crafty.proto.client.JoinRequestCraftyPacket
import world.crafty.proto.registerVertxCraftyCodecs
import world.crafty.proto.server.JoinResponseCraftyPacket
import world.crafty.proto.server.PreSpawnCraftyPacket
import world.crafty.server.world.World

class CraftyServer(val address: String, val world: World) : AbstractVerticle() {
    private var playerIdCounter = 0
    private val playersById = mutableMapOf<Int, CraftyPlayer>()
    
    val players : Collection<CraftyPlayer>
        get() = playersById.values
    
    override fun start() {
        val eb = vertx.eventBus()
        registerVertxCraftyCodecs(eb)
        
        eb.consumer<JoinRequestCraftyPacket>("$address:join") {
            val request = it.body()
            val playerId = ++playerIdCounter
            val player = CraftyPlayer(this, playerId, request.username, request.authMojang, request.authXbox, request.platform, request.skin, world.spawn)
            playersById[playerId] = player
            player.setupConsumers(eb)
            println("(Crafty) ${it.body().username} joined, id $playerId!")
            it.reply(JoinResponseCraftyPacket(playerId, PreSpawnCraftyPacket(
                    entityId = player.entityId,
                    spawnLocation = world.spawn,
                    dimension = 0,
                    gamemode = GameMode.CREATIVE
            )))
        }
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