package world.crafty.pe.session.states

import world.crafty.common.utils.logger
import world.crafty.pe.PeNetworkSession
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.packets.client.ConnectedPingPePacket
import world.crafty.pe.proto.packets.client.NewIncomingConnection

private val log = logger<NewConnectionPeSessionState>()
class NewConnectionPeSessionState(session: PeNetworkSession) : ConnectedPeSessionState(session) {
    suspend override fun handle(packet: PePacket) {
        when(packet) {
            is ConnectedPingPePacket -> {
                onPing(packet)
            }
            is NewIncomingConnection -> {
                log.info { "A client at ${session.address} is now officially connected!" }
                session.switchState(LoginPeSessionState(session))
            }
            else -> {
                unexpectedPacket(packet)
            }
        }
    }
}