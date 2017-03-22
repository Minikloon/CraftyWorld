package world.crafty.server

import io.vertx.core.eventbus.EventBus
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.client.ChatFromClientCraftyPacket
import world.crafty.proto.client.ChunksRadiusRequestCraftyPacket
import world.crafty.proto.server.ChatMessageCraftyPacket
import world.crafty.proto.server.ChunksRadiusResponseCraftyPacket
import world.crafty.server.world.serialize.chunkPacketEncoder
import world.crafty.proto.CraftyChunkColumn

class CraftyPlayer(
        private val server: CraftyServer,
        val id: Int,
        val username: String,
        val authMojang: Boolean,
        val authXbox: Boolean,
        val platform: MinecraftPlatform
) {
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

            val encoder = platform.chunkPacketEncoder

            val columns = world.chunks
                    .filter { (it.x - pos.x() / 16) * (it.x - pos.x() / 16) + (it.z - pos.z() / 16) * (it.z - pos.z() / 16) < radius * radius }
                    .map { encoder.toPacket(it) }

            /*
                val columns = (-radius..radius).flatMap { chunkX ->
                    (-radius..radius).map { chunkZ ->
                        val chunk = CraftyChunkColumn(chunkX + pos.x().toInt() / 16, chunkZ + pos.z().toInt() / 16)
                        if(!(Math.abs(chunkX) == radius || Math.abs(chunkZ) == radius)) {
                            for (y in 0 until 30) {
                                for (z in 0 until 16) {
                                    for (x in 0 until 16) {
                                        chunk.setTypeAndData(x, y, z, 1, 0)
                                    }
                                }
                            }
                        }
                        encoder.toPacket(chunk)
                    }
                }
                */

            it.reply(ChunksRadiusResponseCraftyPacket(columns))
        }
    }
}