package world.crafty.pe.proto

import world.crafty.pe.proto.packets.client.ConnectedPingPePacket
import world.crafty.pe.proto.packets.client.*
import world.crafty.pe.proto.packets.mixed.CompressionWrapperPePacket
import world.crafty.pe.proto.packets.mixed.EncryptionWrapperPePacket
import world.crafty.pe.proto.packets.mixed.PlayerActionPePacket
import world.crafty.pe.proto.packets.mixed.SetPlayerPositionPePacket
import world.crafty.pe.proto.packets.server.*
import world.crafty.pe.raknet.packets.*

object ServerBoundPeRaknetPackets : InboundPePacketList() {
    override fun getCodecs(): List<PePacket.PePacketCodec> {
        return listOf(
                UnconnectedPingClientPePacket.Codec,
                OpenConnectionRequest1PePacket.Codec,
                OpenConnectionRequest2PePacket.Codec
        )
    }
}

object ServerBoundPeTopLevelPackets : InboundPePacketList() {
    override fun getCodecs(): List<PePacket.PePacketCodec> {
        return listOf(
                ConnectionRequestPePacket.Codec,
                NewIncomingConnection.Codec,
                ConnectedPingPePacket.Codec,
                EncryptionWrapperPePacket.Codec,
                LoginPePacket.Codec,
                CompressionWrapperPePacket.Codec
        )
    }
}

object ServerBoundPeWrappedPackets : InboundPePacketList() {
    override fun getCodecs(): List<PePacket.PePacketCodec> {
        return listOf(
                ResourcePackClientResponsePePacket.Codec,
                ChunkRadiusRequestPePacket.Codec,
                PlayerActionPePacket.Codec,
                SetPlayerPositionPePacket.Codec
        )
    }
}