package world.crafty.pc.proto

import world.crafty.pc.proto.packets.client.*

object ServerBoundPcHandshakePackets : InboundPcPacketList() {
    override fun getCodecs() = listOf(
            HandshakePcPacket
    )
}

object ServerBoundPcStatusPackets : InboundPcPacketList() {
    override fun getCodecs() = listOf(
            StatusRequestPcPacket,
            PingPcPacket
    )

}

object ServerBoundPcLoginPackets : InboundPcPacketList() {
    override fun getCodecs() = listOf(
            LoginStartPcPacket,
            EncryptionResponsePcPacket
    )
}

object ServerBoundPcPlayPackets : InboundPcPacketList() {
    override fun getCodecs() = listOf(
            ClientStatusPcPacket,
            ClientChatMessagePcPacket,
            PlayerPosAndLookPcPacket,
            ClientPluginMessagePcPacket,
            ClientSettingsPcPacket,
            PlayerTeleportConfirmPcPacket,
            ClientKeepAlivePcPacket,
            PlayerPositionPcPacket,
            PlayerPosAndLookPcPacket,
            PlayerLookPcPacket
    )
}