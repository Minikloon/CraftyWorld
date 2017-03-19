package world.crafty.server

import io.vertx.core.AbstractVerticle
import world.crafty.proto.client.JoinRequestCraftyPacket
import world.crafty.proto.mixed.ChatMessageCraftyPacket
import world.crafty.proto.registerVertxCraftyCodecs
import world.crafty.proto.server.JoinResponseCraftyPacket

class CraftyServer(val address: String) : AbstractVerticle() {
    private var playerIdCounter = 0
    private val players = mutableMapOf<Int, CraftyPlayer>()
    
    override fun start() {
        val eb = vertx.eventBus()
        registerVertxCraftyCodecs(eb)
        
        eb.consumer<JoinRequestCraftyPacket>("$address:join") {
            println("(Crafty) ${it.body().username} joined!")
            it.reply(JoinResponseCraftyPacket(true))
        }
        
        eb.consumer<ChatMessageCraftyPacket>("$address:chat") {
            println("${it.body().text}")
        }
    }
}