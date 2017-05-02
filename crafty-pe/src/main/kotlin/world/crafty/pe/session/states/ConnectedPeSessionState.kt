package world.crafty.pe.session.states

import world.crafty.common.utils.sinceThen
import world.crafty.pe.PeNetworkSession
import world.crafty.pe.proto.packets.client.ConnectedPingPePacket
import world.crafty.pe.proto.packets.server.ConnectedPongPePacket
import world.crafty.pe.raknet.RakMessageReliability
import world.crafty.pe.session.PeSessionState
import java.time.Duration
import java.time.Instant

open abstract class ConnectedPeSessionState(session: PeNetworkSession) : PeSessionState(session) {
    private var lastPing = Instant.now()
    
    suspend override fun onStart() {
        setPeriodic(5000) {
            if(lastPing.sinceThen() > pingTimeout) // get that in your session state? make sure to catch ConnectedPingPePacket!
                session.disconnect("Timeout >${pingTimeout.toMillis()}ms")
        }
    }
    
    protected fun onPing(packet: ConnectedPingPePacket) {
        val response = ConnectedPongPePacket(packet.pingTimestamp, System.currentTimeMillis())
        lastPing = Instant.now()
        session.send(response, RakMessageReliability.UNRELIABLE)
    }
    
    open val pingTimeout: Duration = Duration.ofMillis(5500)
}