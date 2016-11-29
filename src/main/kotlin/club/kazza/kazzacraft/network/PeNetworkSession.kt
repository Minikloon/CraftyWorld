package club.kazza.kazzacraft.network

import club.kazza.kazzacraft.network.protocol.*
import club.kazza.kazzacraft.network.raknet.RakDatagramFlags
import club.kazza.kazzacraft.network.raknet.RakMessageFlags
import club.kazza.kazzacraft.network.raknet.RaknetReliability
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.net.SocketAddress
import java.io.ByteArrayOutputStream
import java.util.*

class PeNetworkSession(val socket: DatagramSocket, val address: SocketAddress) : AbstractVerticle() {
    fun handleDatagram(buffer: Buffer) {
        val mcStream = MinecraftInputStream(buffer.bytes)

        val header = RakDatagramFlags(mcStream.readByte())
        if(header.userPacket) {
            println("user packet ${header.packetId}")
            if(header.ack) {
                handleAcknowledge(mcStream)
            }
            else if(header.nack) {
                handleNotAcknowledge(mcStream)
            }
            else {
                handleConnectedUserPacket(header, mcStream)
            }
        }
        else {
            handleRakNetPacket(header, mcStream)
        }
    }

    private fun handleAcknowledge(mcStream: MinecraftInputStream) {
        val ack = AckPePacket.Codec.deserialize(mcStream) as AckPePacket
        val ackedCount = ack.ranges.sumBy { it.count() }
        println("ack $ackedCount datagrams")
    }

    private fun handleNotAcknowledge(mcStream: MinecraftInputStream) {
        println("nack")
    }

    private fun handleConnectedUserPacket(datagramHeader: RakDatagramFlags, mcStream: MinecraftInputStream) {
        val datagramSeqNo = mcStream.read3BytesInt()
        val messages = ArrayList<ByteArray>(6)
        while(mcStream.available() != 0) {
            val msgHeader = RakMessageFlags(mcStream.readByte())
            val bitsLength = mcStream.readUnsignedShort()
            val messageSize = Math.ceil(bitsLength.toDouble() / 8).toInt()

            if (msgHeader.reliability != RaknetReliability.RELIABLE)
                throw NotImplementedError("Unsupported RakNet Reliability: ${msgHeader.reliability}")
            if (msgHeader.hasSplit)
                throw NotImplementedError("Unsupported RakNet split messages")

            val messageNo = mcStream.read3BytesInt()
            val message = ByteArray(messageSize)
            mcStream.read(message)
            messages.add(message)
        }
        println("received ${messages.size} messages")

        messages.forEach { handleMessage(MinecraftInputStream(it)) }
    }

    private fun handleMessage(mcStream: MinecraftInputStream) {
        val id = mcStream.read()
        println("message id $id")
    }

    private fun handleRakNetPacket(header: RakDatagramFlags, mcStream: MinecraftInputStream) {
        println("raknet packet ${header.packetId}")
        val codec = InboundPeRaknetPackets.idToCodec[header.packetId]
        if(codec == null) {
            println("Unknown pe raknet packet id ${header.packetId}")
            return
        }

        val packet = codec.deserialize(mcStream)
        handleUnconnectedPacket(packet)
    }

    private fun handleUnconnectedPacket(packet: PePacket) {
        when(packet) {
            is UnconnectedPingClientPePacket -> {
                println("unconnected ping from $address")
                val serverId = address.hashCode().toLong()
                val reply = UnconnectedPongServerPePacket(packet.pingId, serverId, "Server")
                send(reply)
            }
            is OpenConnectionRequest1PePacket -> {
                println("open connection request 1 mtu ${packet.mtuSize}")
                val reply = OpenConnectionReply1PePacket(1234, false, packet.mtuSize)
                send(reply)
            }
            is OpenConnectionRequest2PePacket -> {
                println("open connection request 2")
                val reply = OpenConnectionReply2PePacket(1234, address, packet.mtuSize, false)
                send(reply)
            }
            else -> {
                println("unhandled packet ${packet.javaClass.name}")
            }
        }
    }

    fun send(packet: PePacket) {
        val byteStream = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(byteStream)
        mcStream.writeByte(packet.id)
        packet.serialize(mcStream)

        socket.send(Buffer.buffer(byteStream.toByteArray()), address.port(), address.host()) {}
    }
}