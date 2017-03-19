package world.crafty.proto

import io.vertx.core.eventbus.EventBus
import world.crafty.proto.client.ChatFromClientCraftyPacket
import world.crafty.proto.client.JoinRequestCraftyPacket
import world.crafty.proto.server.ChatMessageCraftyPacket
import world.crafty.proto.server.JoinResponseCraftyPacket

fun registerVertxCraftyCodecs(eb: EventBus) {
    eb.registerDefaultCodec(JoinRequestCraftyPacket::class.java, CraftyVertxCodec(JoinRequestCraftyPacket::class.java, JoinRequestCraftyPacket.Codec))
    eb.registerDefaultCodec(JoinResponseCraftyPacket::class.java, CraftyVertxCodec(JoinResponseCraftyPacket::class.java, JoinResponseCraftyPacket.Codec))
    eb.registerDefaultCodec(ChatFromClientCraftyPacket::class.java, CraftyVertxCodec(ChatFromClientCraftyPacket::class.java, ChatFromClientCraftyPacket.Codec))
    eb.registerDefaultCodec(ChatMessageCraftyPacket::class.java, CraftyVertxCodec(ChatMessageCraftyPacket::class.java, ChatMessageCraftyPacket.Codec))
}