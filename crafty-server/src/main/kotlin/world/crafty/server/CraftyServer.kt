package world.crafty.server

import io.vertx.core.AbstractVerticle
import world.crafty.common.utils.logger
import world.crafty.common.vertx.typedSend
import world.crafty.proto.GameMode
import world.crafty.proto.packets.client.JoinRequestCraftyPacket
import world.crafty.proto.registerVertxCraftyCodecs
import world.crafty.proto.metadata.MetaFieldRegistry
import world.crafty.proto.metadata.registerBuiltInMetaDefinitions
import world.crafty.proto.packets.client.QuitCraftyPacket
import world.crafty.proto.packets.server.*
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
            val packet = it.body()
            val playerId = ++playerIdCounter
            val craftyPlayer = CraftyPlayer(this, eb, playerId, packet.username, packet.authMojang, packet.authXbox, packet.platform, packet.skin, world.spawn)
            playersById[playerId] = craftyPlayer
            log.info { "(Crafty) ${it.body().username} joined, id $playerId!" }
            it.reply(JoinResponseCraftyPacket(playerId, PreSpawnCraftyPacket(
                    entityId = craftyPlayer.entityId,
                    spawnLocation = world.spawn,
                    dimension = 0,
                    gamemode = GameMode.SURVIVAL
            )))
        }
        
        eb.consumer<QuitCraftyPacket>("$address:quit") {
            val packet = it.body()
            disconnectPlayer(packet.craftyPlayerId, "Quit!", notify = false)
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
    
    fun disconnectPlayer(playerId: Int, message: String = "Disconnected by server", notify: Boolean = true) {
        val player = playersById[playerId] ?: return
        playersById.remove(playerId)
        log.info { "Disconnected crafty player ${player.username} ($message)" }
        player.onDisconnect()
        if(notify)
            player.send(DisconnectPlayerCraftyPacket(message))
    }
}