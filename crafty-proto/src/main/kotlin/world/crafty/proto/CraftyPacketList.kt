package world.crafty.proto

import io.vertx.core.eventbus.EventBus
import world.crafty.proto.client.ChatFromClientCraftyPacket
import world.crafty.proto.client.ChunksRadiusRequestCraftyPacket
import world.crafty.proto.client.JoinRequestCraftyPacket
import world.crafty.proto.server.ChatMessageCraftyPacket
import world.crafty.proto.server.ChunksRadiusResponseCraftyPacket
import world.crafty.proto.server.JoinResponseCraftyPacket
import world.crafty.proto.server.PreSpawnCraftyPacket

fun registerVertxCraftyCodecs(eb: EventBus) {
    eb.registerDefaultCodec(JoinRequestCraftyPacket::class.java, CraftyVertxCodec(JoinRequestCraftyPacket::class.java, JoinRequestCraftyPacket.Codec))
    eb.registerDefaultCodec(JoinResponseCraftyPacket::class.java, CraftyVertxCodec(JoinResponseCraftyPacket::class.java, JoinResponseCraftyPacket.Codec))
    eb.registerDefaultCodec(ChatFromClientCraftyPacket::class.java, CraftyVertxCodec(ChatFromClientCraftyPacket::class.java, ChatFromClientCraftyPacket.Codec))
    eb.registerDefaultCodec(ChatMessageCraftyPacket::class.java, CraftyVertxCodec(ChatMessageCraftyPacket::class.java, ChatMessageCraftyPacket.Codec))
    eb.registerDefaultCodec(PreSpawnCraftyPacket::class.java, CraftyVertxCodec(PreSpawnCraftyPacket::class.java, PreSpawnCraftyPacket.Codec))
    eb.registerDefaultCodec(ChunksRadiusRequestCraftyPacket::class.java, CraftyVertxCodec(ChunksRadiusRequestCraftyPacket::class.java, ChunksRadiusRequestCraftyPacket.Codec))
    eb.registerDefaultCodec(ChunksRadiusResponseCraftyPacket::class.java, CraftyVertxCodec(ChunksRadiusResponseCraftyPacket::class.java, ChunksRadiusResponseCraftyPacket.Codec))
}