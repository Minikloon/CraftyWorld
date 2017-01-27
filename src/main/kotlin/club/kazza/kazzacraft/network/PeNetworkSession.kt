package club.kazza.kazzacraft.network

import club.kazza.kazzacraft.network.protocol.*
import club.kazza.kazzacraft.network.raknet.*
import club.kazza.kazzacraft.network.raknet.RakMessageReliability.*
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import club.kazza.kazzacraft.utils.toHexStr
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.net.SocketAddress
import io.vertx.core.net.impl.SocketAddressImpl
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class PeNetworkSession(val socket: DatagramSocket, val address: SocketAddress) : AbstractVerticle() {
    private val receiveQueue = ConcurrentLinkedQueue<Buffer>()
    private var datagramSeqNo: Int = 0
    private var messageSeqNo: Int = 0
    private var messageOrderIndex: Int = 0
    private var encrypted: Boolean = false
    private val incompleteSplits = mutableMapOf<Short, SplittedMessage>()
    private val orderingChannels = mutableMapOf<Byte, OrderingChannel>()
    
    override fun start() {
        vertx.setPeriodic(2500) { _ -> pruneSplittedDatagrams() }
        vertx.setPeriodic(10) { _ -> processDatagramReceiveQueue() }
        vertx.setPeriodic(50) { _ -> processOrderingChannels() }
    }
    
    private fun pruneSplittedDatagrams() {
        incompleteSplits.values.removeIf { System.currentTimeMillis() - it.timestamp > 5000L }
    }
    
    fun queueDatagramReceive(buffer: Buffer) {
        receiveQueue.add(buffer)
    }
    
    private fun processDatagramReceiveQueue() {
        while(true) {
            val datagram = receiveQueue.poll() ?: break
            decodeRaknetDatagram(datagram)
        }
    }

    private fun decodeRaknetDatagram(buffer: Buffer) {
        val mcStream = MinecraftInputStream(buffer.bytes)

        val headerFlags = RakDatagramFlags(mcStream.readByte())
        if(headerFlags.userPacket) {
            println("${System.currentTimeMillis()} IN packet ${headerFlags.packetId.toByte().toHexStr()}")
            if(headerFlags.ack) {
                handleRakAcknowledge(mcStream)
            }
            else if(headerFlags.nack) {
                handleRakNotAcknowledge(mcStream)
            }
            else {
                val datagramSeqNo = mcStream.read3BytesInt()
                val datagram = RakConnectedDatagram(headerFlags, datagramSeqNo, Buffer.buffer(mcStream.readRemainingBytes()))
                decodeConnectedDatagram(datagram)
            }
        }
        else {
            println("${System.currentTimeMillis()} IN rak packet ${headerFlags.packetId.toByte().toHexStr()}")
            handleRakNetPacket(headerFlags, mcStream)
        }
    }

    private fun handleRakAcknowledge(mcStream: MinecraftInputStream) {
        val ack = AckPePacket.Codec.deserialize(mcStream) as AckPePacket
        val ackedCount = ack.datagramSeqNos.count()
        println("ack $ackedCount datagrams")
    }

    private fun handleRakNotAcknowledge(mcStream: MinecraftInputStream) {
        println("nack")
    }

    private val supportedReliabilities = setOf(RELIABLE, RELIABLE_ORDERED, UNRELIABLE)
    
    private fun decodeConnectedDatagram(datagram: RakConnectedDatagram) {
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
                    println("Completed a split message")
                    messages.add(full)
                }
            }
            else {
                messages.add(message)
            }
        }
        println("datagram with ${messages.size} full message")

        messages.forEach {
            if(it.headerFlags.reliability.ordered) {
                val metaOrder = it.order!!
                val channel = metaOrder.channel
                if(channel >= 100) throw IllegalStateException("Ordering channel too high!")
                val orderChannel = orderingChannels.computeIfAbsent(channel) { OrderingChannel(channel) }
                orderChannel.add(it)
            }
            else {
                handlePeMessage(it.dataAsStream)
            }
        }
    }
    
    fun processOrderingChannels() {
        orderingChannels.values.forEach { 
            val message = it.poll() ?: return@forEach
            handlePeMessage(message.dataAsStream)
        }
    }

    private fun handlePeMessage(mcStream: MinecraftInputStream) {
        val id = mcStream.read()
        val codec = InboundPePackets.idToCodec[id]
        if(codec == null) {
            println("Unknown pe message id $id")
            return
        }

        val message = codec.deserialize(mcStream)
        when(message) {
            is ConnectedPingPePacket -> {
                val response = ConnectedPongPePacket(message.pingTimestamp, System.currentTimeMillis())
                sendConnected(response, UNRELIABLE)
            }
            is NewIncomingConnection -> {
                println("A client at ${address.host()} is now officially connected!")
            }
            is ConnectionRequestPePacket -> {
                println("connected connection request...")
                val response = ConnectionRequestAcceptPePacket(
                        systemAddress = SocketAddressImpl(19132, "127.0.0.1"),
                        systemIndex = 0,
                        systemAddresses = Array(10) { if(it == 0) SocketAddressImpl(19132, "0.0.0.0") else SocketAddressImpl(19132, "127.0.0.1") },
                        incommingTimestamp = message.timestamp,
                        serverTimestamp = System.currentTimeMillis()
                )
                sendConnected(response, RELIABLE)
            }
            is EncryptionWrapperPePacket -> {
                println("wrapper!")
                if(encrypted) {
                    throw NotImplementedError("Encryption not supported yet!")
                } else {
                    val payloadStream = MinecraftInputStream(message.payload)
                    handlePeMessage(payloadStream)
                }
            }
            is LoginPePacket -> {
                println("Login from $address ${message.protocolVersion} ${message.edition} ${message.payload.size}")
            }
            else -> {
                println("Unhandled pe message ${message.javaClass.simpleName}")
            }
        }
    }

    private fun handleRakNetPacket(header: RakDatagramFlags, mcStream: MinecraftInputStream) {
        val codec = InboundPeRaknetPackets.idToCodec[header.packetId]
        if(codec == null) {
            println("Unknown pe raknet packet id ${header.packetId}")
            return
        }

        val packet = codec.deserialize(mcStream)
        handleUnconnectedPacket(packet)
    }

    private var count = 0
    private fun handleUnconnectedPacket(packet: PePacket) {
        when(packet) {
            is UnconnectedPingClientPePacket -> {
                println("unconnected ping from $address")
                val serverId = address.hashCode().toLong()
                val color = if(count % 2 == 0) "§c" else "§4"
                val motdFirstLine = "$color❤ §a§lHELLO WORLD $color❤"
                val motdSecondLine = "§eCONNECT ${count++}"
                val onlinePlayers = 0
                val maxPlayers = 1000
                val serverListData = "MCPE;$motdFirstLine;100;1.0.0.7;$onlinePlayers;$maxPlayers;$serverId;$motdSecondLine;Survival;"
                val reply = UnconnectedPongServerPePacket(packet.pingId, serverId, serverListData)
                sendUnconnected(reply)
            }
            is OpenConnectionRequest1PePacket -> {
                println("open connection request 1 mtu ${packet.mtuSize}")
                val reply = OpenConnectionReply1PePacket(1234, false, packet.mtuSize)
                sendUnconnected(reply)
            }
            is OpenConnectionRequest2PePacket -> {
                println("open connection request 2")
                val reply = OpenConnectionReply2PePacket(1234, address, packet.mtuSize, false)
                sendUnconnected(reply)
            }
            else -> {
                println("unhandled packet ${packet.javaClass.name}")
            }
        }
    }

    fun queueAck(seqNo: Int) {
        val ack = AckPePacket(listOf(seqNo)) // TODO: merge acks over a few ms to save bandwidth
        sendUnconnected(ack)
    }

    fun sendUnconnected(packet: PePacket) {
        val byteStream = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(byteStream)
        mcStream.writeByte(packet.id)
        packet.serialize(mcStream)

        println("${System.currentTimeMillis()} OUT unconnected ${packet.javaClass.simpleName} ${packet.id.toByte().toHexStr()}")
        socket.send(Buffer.buffer(byteStream.toByteArray()), address.port(), address.host()) {}
    }

    fun sendConnected(packet: PePacket, reliability: RakMessageReliability = RELIABLE_ORDERED) {
        // TODO support raknet multi-message datagram && multi-datagram message
        val byteStream = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(byteStream)
        mcStream.writeByte(packet.id)
        packet.serialize(mcStream)
        val packetBuffer = byteStream.toByteArray()

        val dByteStream = ByteArrayOutputStream()
        val dMcStream = MinecraftOutputStream(dByteStream)

        dMcStream.writeByte(RakDatagramFlags.nonContinuousUserDatagram)
        dMcStream.write3BytesInt(datagramSeqNo++)

        val message = RakMessage(
                RakMessageFlags(reliability, hasSplit = false),
                if(reliability.reliable) RakMessage.MetaReliability(messageSeqNo++) else null,
                if(reliability.ordered) RakMessage.MetaOrder(messageOrderIndex++, channel = 0) else null,
                null,
                packetBuffer
        )
        message.serialize(dMcStream)

        println("${System.currentTimeMillis()} OUT connected ${packet.javaClass.simpleName} ${packet.id.toByte().toHexStr()}")
        socket.send(Buffer.buffer(dByteStream.toByteArray()), address.port(), address.host()) {}
    }
}