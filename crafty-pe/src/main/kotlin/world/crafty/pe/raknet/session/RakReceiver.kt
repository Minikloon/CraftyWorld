package world.crafty.pe.raknet.session

import io.vertx.core.Vertx
import io.vertx.core.datagram.DatagramPacket
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.utils.getLogger
import world.crafty.common.utils.trace
import world.crafty.common.utils.warn
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.ServerBoundPeRaknetPackets
import world.crafty.pe.proto.packets.client.UnconnectedPingClientPePacket
import world.crafty.pe.raknet.*
import world.crafty.pe.raknet.packets.*
import java.util.ArrayList
import java.util.concurrent.ConcurrentLinkedQueue

private val log = getLogger<RakReceiver>()
class RakReceiver(val session: RakNetworkSession, val onAck: (AckPePacket) -> Unit, val onNack: () -> Unit, val handlePayload: (MinecraftInputStream) -> Unit) {
    private val incomingQueue = ConcurrentLinkedQueue<DatagramPacket>()

    private val incompleteSplits = mutableMapOf<Short, SplittedMessage>()
    private val orderingChannels = mutableMapOf<Byte, OrderingChannel>()

    private val ackBuffer = mutableListOf<Int>()
    
    fun registerWithVertx(vertx: Vertx) {
        vertx.setPeriodic(10) { _ -> processDatagramReceiveQueue() }
        vertx.setPeriodic(10) { _ -> processOrderingChannels() }
        vertx.setPeriodic(10) { _ -> flushAckBuffer() }
        vertx.setPeriodic(2500) { _ -> pruneSplittedDatagrams() }
    }
    
    fun onReceiveDatagram(datagram: DatagramPacket) {
        incomingQueue.add(datagram)
    }

    private fun processDatagramReceiveQueue() {
        while(true) {
            val datagram = incomingQueue.poll() ?: break
            val buffer = datagram.data().bytes
            decodeDatagramBuffer(buffer)
        }
    }

    private fun decodeDatagramBuffer(buffer: ByteArray) {
        val mcStream = MinecraftInputStream(buffer)

        val headerFlags = RakDatagramFlags(mcStream.readByte())
        if(headerFlags.userPacket) {
            if(headerFlags.ack) {
                val ack = AckPePacket.Codec.deserialize(mcStream) as AckPePacket
                onAck(ack)
            }
            else if(headerFlags.nack) {
                onNack()
            }
            else {
                val datagramSeqNo = mcStream.read3BytesInt()
                val datagram = RakDatagram(headerFlags, datagramSeqNo, mcStream.readRemainingBytes())
                processConnectedDatagram(datagram)
            }
        }
        else {
            handleRakNetPacket(headerFlags, mcStream)
        }
    }

    private val supportedReliabilities = setOf(RakMessageReliability.RELIABLE, RakMessageReliability.RELIABLE_ORDERED, RakMessageReliability.UNRELIABLE)
    private fun processConnectedDatagram(datagram: RakDatagram) {
        val mcStream = datagram.dataAsStream

        queueAck(datagram.sequenceNumber)

        val messages = ArrayList<RakMessage>(6)
        while(mcStream.available() != 0) {
            val message = RakMessage.Codec.deserialize(mcStream)

            val reliability = message.headerFlags.reliability
            if(! supportedReliabilities.contains(reliability))
                throw NotImplementedError("$reliability message reliability is not supported!")

            if(message.headerFlags.hasSplit) {
                val (splitsCount, splitsId, splitIndex) = message.splits!!

                val splitMessage = incompleteSplits.computeIfAbsent(splitsId) { SplittedMessage(splitsId, splitsCount) }
                splitMessage.addSplit(splitIndex, message)

                val full = splitMessage.full
                if(full != null) {
                    messages.add(full)
                    incompleteSplits.remove(splitsId)
                }
            }
            else {
                messages.add(message)
            }
        }

        messages.forEach {
            if(it.headerFlags.reliability.ordered) {
                val metaOrder = it.order!!
                val channel = metaOrder.channel
                if(channel >= 100) throw IllegalStateException("Ordering channel too high!")
                val orderChannel = orderingChannels.computeIfAbsent(channel) { OrderingChannel(channel) }
                orderChannel.add(it)
            }
            else {
                handlePayload(it.dataAsStream)
            }
        }
    }

    private fun handleRakNetPacket(header: RakDatagramFlags, mcStream: MinecraftInputStream) {
        val codec = ServerBoundPeRaknetPackets.idToCodec[header.packetId.toInt()]
        if(codec == null) {
            log.warn { "Unknown pe raknet packet id ${header.packetId}" }
            return
        }

        val packet = codec.deserialize(mcStream)
        when(packet) {
            is UnconnectedPingClientPePacket -> {
                val serverId = session.address.hashCode().toLong()
                val extra = session.getUnconnectedPongExtraData(serverId)
                val reply = UnconnectedPongServerPePacket(packet.pingId, serverId, extra)
                session.sendRaw(reply)
            }
            is OpenConnectionRequest1PePacket -> {
                log.trace { "open connection request 1 mtu ${packet.mtuSize}" }
                session.mtuSize = packet.mtuSize - 68
                val reply = OpenConnectionReply1PePacket(1234, false, packet.mtuSize)
                session.sendRaw(reply)
            }
            is OpenConnectionRequest2PePacket -> {
                log.trace { "open connection request 2" }
                val reply = OpenConnectionReply2PePacket(1234, session.address, packet.mtuSize, false)
                session.sendRaw(reply)
            }
            else -> {
                log.warn { "unhandled raknet packet ${packet.javaClass.name}" }
            }
        }
    }

    private fun processOrderingChannels() {
        orderingChannels.values.forEach {
            val message = it.poll() ?: return@forEach
            handlePayload(message.dataAsStream)
        }
    }

    private fun queueAck(seqNo: Int) {
        ackBuffer.add(seqNo)
    }

    private fun flushAckBuffer() {
        if(ackBuffer.isEmpty()) return
        val ack = AckPePacket(ackBuffer)
        session.sendRaw(ack.serializedWithId())
        ackBuffer.clear()
    }

    private fun pruneSplittedDatagrams() {
        incompleteSplits.values.removeIf { System.currentTimeMillis() - it.timestamp > 5000L }
    }
}