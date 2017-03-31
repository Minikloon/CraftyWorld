package world.crafty.server

import io.vertx.core.eventbus.EventBus
import world.crafty.common.vertx.typedConsumer
import world.crafty.common.vertx.typedSend
import world.crafty.proto.CraftySkin
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.client.*
import world.crafty.proto.server.*
import java.util.*

class CraftyPlayer(
        private val server: CraftyServer,
        val id: Int,
        val username: String,
        val authMojang: Boolean,
        val authXbox: Boolean,
        val platform: MinecraftPlatform,
        val skin: CraftySkin
) {
    val uuid: UUID = UUID.randomUUID()
    
    fun setupConsumers(eb: EventBus) {
        eb.consumer<ChatFromClientCraftyPacket>("p:s:$id:chat") {
            val body = it.body()
            val text = "$username > ${body.text}"
            server.players.forEach {
                eb.send("p:c:${it.id}:chat", ChatMessageCraftyPacket(text))
            }
        }

        eb.consumer<ChunksRadiusRequestCraftyPacket>("p:s:$id:chunkRadiusReq") {
            val body = it.body()
            val radius = body.radius
            val pos = body.pos
            val world = server.world
            
            val columns = world.chunks
                    .filter { (it.x - pos.x() / 16) * (it.x - pos.x() / 16) + (it.z - pos.z() / 16) * (it.z - pos.z() / 16) < radius * radius }
                    .map { it.toPacket() }
            
            it.reply(ChunksRadiusResponseCraftyPacket(columns))
        }
        
        eb.typedConsumer("p:s:$id", ReadyToSpawnCraftyPacket::class) {
            val everyoneOnline = UpdatePlayerListCraftyPacket(
                    items = server.players.map { player ->
                        PlayerListAdd(
                                uuid = player.uuid,
                                entityId = player.id.toLong(),
                                name = player.username,
                                ping = 30,
                                skin = player.skin
                        )
                    }
            )
            
            eb.typedSend("p:c:$id", everyoneOnline)
            
            val plusNewPlayer = UpdatePlayerListCraftyPacket(
                    items = listOf(
                            PlayerListAdd(
                                    uuid = uuid,
                                    entityId = id.toLong(),
                                    name = username,
                                    ping = 30,
                                    skin = skin
                            )
                    )
            )
            
            server.typedSendAllExcept(this, plusNewPlayer)
            
            eb.typedSend("p:c:$id", SpawnSelfCraftyPacket())
            println("Crafty spawning player $username!")
        }
    }
}