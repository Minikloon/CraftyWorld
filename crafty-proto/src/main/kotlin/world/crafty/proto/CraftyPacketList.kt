package world.crafty.proto

import io.vertx.core.eventbus.EventBus
import world.crafty.proto.packets.client.*
import world.crafty.proto.packets.server.*

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
    eb.registerDefaultCodec<AddPlayerCraftyPacket>(AddPlayerCraftyPacket.Codec)
    eb.registerDefaultCodec<PlayerActionCraftyPacket>(PlayerActionCraftyPacket.Codec)
    eb.registerDefaultCodec<PatchEntityCraftyPacket>(PatchEntityCraftyPacket.Codec)
    eb.registerDefaultCodec<SetPlayerLookCraftyPacket>(SetPlayerLookCraftyPacket.Codec)
    eb.registerDefaultCodec<SetPlayerPosAndLookCraftyPacket>(SetPlayerPosAndLookCraftyPacket.Codec)
    eb.registerDefaultCodec<SetPlayerPosCraftyPacket>(SetPlayerPosCraftyPacket.Codec)
    eb.registerDefaultCodec<SetEntityLocationCraftyPacket>(SetEntityLocationCraftyPacket.Codec)
}

private inline fun <reified T : CraftyPacket> EventBus.registerDefaultCodec(codec: CraftyPacket.CraftyPacketCodec) {
    val clazz = T::class.java
    registerDefaultCodec<T>(clazz, CraftyVertxCodec(clazz, codec))
}