package world.crafty.proto

import io.vertx.core.eventbus.EventBus
import world.crafty.proto.client.ChatFromClientCraftyPacket
import world.crafty.proto.client.ChunksRadiusRequestCraftyPacket
import world.crafty.proto.client.JoinRequestCraftyPacket
import world.crafty.proto.client.ReadyToSpawnCraftyPacket
import world.crafty.proto.server.*

private var registered = false
private val lock = Any()

fun registerVertxCraftyCodecs(eb: EventBus) {
    synchronized(lock) {
        if(registered) return
        registered = true
    }
    
    eb.registerDefaultCodec<JoinRequestCraftyPacket>(JoinRequestCraftyPacket.Codec)
    eb.registerDefaultCodec<JoinResponseCraftyPacket>(JoinResponseCraftyPacket.Codec)
    eb.registerDefaultCodec<ChatFromClientCraftyPacket>(ChatFromClientCraftyPacket.Codec)
    eb.registerDefaultCodec<ChatMessageCraftyPacket>(ChatMessageCraftyPacket.Codec)
    eb.registerDefaultCodec<PreSpawnCraftyPacket>(PreSpawnCraftyPacket.Codec)
    eb.registerDefaultCodec<ChunksRadiusRequestCraftyPacket>(ChunksRadiusRequestCraftyPacket.Codec)
    eb.registerDefaultCodec<ChunksRadiusResponseCraftyPacket>(ChunksRadiusResponseCraftyPacket.Codec)
    eb.registerDefaultCodec<ReadyToSpawnCraftyPacket>(ReadyToSpawnCraftyPacket.Codec)
    eb.registerDefaultCodec<UpdatePlayerListCraftyPacket>(UpdatePlayerListCraftyPacket.Codec)
    eb.registerDefaultCodec<SpawnSelfCraftyPacket>(SpawnSelfCraftyPacket.Codec)
}

private inline fun <reified T : CraftyPacket> EventBus.registerDefaultCodec(codec: CraftyPacket.CraftyPacketCodec) {
    val clazz = T::class.java
    registerDefaultCodec<T>(clazz, CraftyVertxCodec(clazz, codec))
}