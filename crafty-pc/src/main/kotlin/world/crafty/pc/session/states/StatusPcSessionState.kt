package world.crafty.pc.session.states

import world.crafty.common.utils.logger
import world.crafty.pc.PcNetworkSession
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.proto.ServerBoundPcStatusPackets
import world.crafty.pc.proto.packets.client.PingPcPacket
import world.crafty.pc.proto.packets.client.StatusRequestPcPacket
import world.crafty.pc.proto.packets.server.PongPcPacket
import world.crafty.pc.proto.packets.server.StatusResponsePcPacket
import world.crafty.pc.session.PcSessionState

private val log = logger<StatusPcSessionState>()
class StatusPcSessionState(session: PcNetworkSession) : PcSessionState(session) {
    override val packetList = ServerBoundPcStatusPackets
    
    suspend override fun handle(packet: PcPacket) {
        when (packet) {
            is StatusRequestPcPacket -> {
                log.info { "Received server list request from ${session.address}" }

                val lpr = StatusResponsePcPacket(
                            StatusResponsePcPacket.ServerVersion(
                                    name = "§l1.13",
                                    protocol = 384
                            ), StatusResponsePcPacket.PlayerStatus(
                            max = 1337,
                            online = 1336
                        ), StatusResponsePcPacket.Description(
                            text = "first line \nsecond line"
                            )
                        )

                session.send(lpr)
            }
            is PingPcPacket -> {
                session.send(PongPcPacket(packet.epoch))
                session.switchState(HandshakePcSessionState(session))
            }
            else -> { 
                log.error { "Unhandled Status packet ${packet.javaClass.simpleName}" }
            }
        }
    }
}