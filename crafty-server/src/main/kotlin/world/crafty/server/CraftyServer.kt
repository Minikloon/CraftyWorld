package world.crafty.server

import io.vertx.core.AbstractVerticle
import world.crafty.proto.client.JoinRequestCraftyPacket
import world.crafty.proto.client.ChatFromClientCraftyPacket
import world.crafty.proto.registerVertxCraftyCodecs
import world.crafty.proto.server.ChatMessageCraftyPacket
import world.crafty.proto.server.JoinResponseCraftyPacket

class CraftyServer(val address: String) : AbstractVerticle() {
    private var playerIdCounter = 0
    private val players = mutableMapOf<Int, CraftyPlayer>()
    
    override fun start() {
        val eb = vertx.eventBus()
        registerVertxCraftyCodecs(eb)
        
        eb.consumer<JoinRequestCraftyPacket>("$address:join") {
            val request = it.body()
            val playerId = ++playerIdCounter
            val player = CraftyPlayer(playerId, request.username, request.authMojang, request.authXbox)
            players[playerId] = player
            println("(Crafty) ${it.body().username} joined, id $playerId!")
            it.reply(JoinResponseCraftyPacket(true, playerId))
        }
        
        eb.consumer<ChatFromClientCraftyPacket>("$address:chat") {
            val body = it.body()
            val sender = players[body.senderId] ?: return@consumer
            val text = "${sender.username} > ${body.text}"
            players.values.forEach { 
                eb.send("p:${it.id}:chat", ChatMessageCraftyPacket(text))
            }
        }
    }
}