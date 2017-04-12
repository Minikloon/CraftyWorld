package world.crafty.pc.session.states

import world.crafty.common.utils.logger
import world.crafty.pc.PcNetworkSession
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.proto.ServerBoundPcHandshakePackets
import world.crafty.pc.proto.packets.client.HandshakePcPacket
import world.crafty.pc.session.PcSessionState

private val log = logger<HandshakePcSessionState>()
class HandshakePcSessionState(session: PcNetworkSession) : PcSessionState(session) {
    override val packetList = ServerBoundPcHandshakePackets
    
    suspend override fun handle(packet: PcPacket) {
        when (packet) {
            is HandshakePcPacket -> {
                log.info { "Received handshake from ${session.address}" }
                session.switchState(when(packet.nextState) {
                    1 -> StatusPcSessionState(session)
                    2 -> FirstPacketLoginStage(session)
                    else -> {
                        log.warn { "Invalid handshake state for ${session.address}" }
                        session.close()
                        return
                    }
                })
            }
            else -> {
                log.error { "Unhandled Handshake packet ${packet.javaClass.simpleName}" }
            }
        }
    }
}