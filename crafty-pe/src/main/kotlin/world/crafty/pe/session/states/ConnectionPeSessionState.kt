package world.crafty.pe.session.states

import io.vertx.core.net.impl.SocketAddressImpl
import world.crafty.common.utils.logger
import world.crafty.pe.PeNetworkSession
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.packets.client.ConnectedPingPePacket
import world.crafty.pe.proto.packets.client.ConnectionRequestPePacket
import world.crafty.pe.proto.packets.server.ConnectionRequestAcceptPePacket
import world.crafty.pe.raknet.RakMessageReliability
import world.crafty.pe.session.PeSessionState

private val log = logger<ConnectionPeSessionState>()
class ConnectionPeSessionState(session: PeNetworkSession) : PeSessionState(session) {
    suspend override fun onStart() {
        super.onStart()
    }
    
    suspend override fun handle(packet: PePacket) {
        when(packet) {
            is ConnectedPingPePacket -> {
                session.disconnect("Premature connected ping")
            }
            is ConnectionRequestPePacket -> {
                val response = ConnectionRequestAcceptPePacket(
                        systemAddress = SocketAddressImpl(19132, "127.0.0.1"),
                        systemIndex = 0,
                        systemAddresses = Array(10) { if (it == 0) SocketAddressImpl(19132, "0.0.0.0") else SocketAddressImpl(19132, "127.0.0.1") },
                        incommingTimestamp = packet.timestamp,
                        serverTimestamp = System.currentTimeMillis()
                )
                session.send(response, RakMessageReliability.RELIABLE)
                session.switchState(NewConnectionPeSessionState(session))
            }
            else -> {
                unexpectedPacket(packet)
            }
        }
    }
}