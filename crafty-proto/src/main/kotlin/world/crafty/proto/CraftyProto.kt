package world.crafty.proto

import io.vertx.core.eventbus.EventBus
import world.crafty.proto.client.JoinRequestCraftyPacket
import world.crafty.proto.server.JoinResponseCraftyPacket

fun registerVertxCraftyCodecs(eb: EventBus) {
    eb.registerDefaultCodec(JoinRequestCraftyPacket::class.java, CraftyVertxCodec(JoinRequestCraftyPacket::class.java, JoinRequestCraftyPacket.Codec))
    eb.registerDefaultCodec(JoinResponseCraftyPacket::class.java, CraftyVertxCodec(JoinResponseCraftyPacket::class.java, JoinResponseCraftyPacket.Codec))
}