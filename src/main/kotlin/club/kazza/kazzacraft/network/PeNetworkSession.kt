package club.kazza.kazzacraft.network

import club.kazza.kazzacraft.network.protocol.*
import club.kazza.kazzacraft.network.raknet.RakDatagramFlags
import club.kazza.kazzacraft.network.raknet.RakMessageFlags
import club.kazza.kazzacraft.network.raknet.RaknetReliability
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
    private val incompleteSplits = mutableMapOf<Int, SplittedMessage>()
    private var encrypted: Boolean = false

    override fun start() {
        vertx.setPeriodic(2500) { _ -> pruneSplits() }
        vertx.setPeriodic(10) { _ -> handleReceiveQueue() }
    }
    
    private fun pruneSplits() {
        incompleteSplits.values.removeIf { System.currentTimeMillis() - it.timestamp > 5000L }
    }
    
    fun queueReceive(buffer: Buffer) {
        receiveQueue.add(buffer)
    }
    
    private fun handleReceiveQueue() {
        while(true) {
            val datagram = receiveQueue.poll() ?: break
            handleDatagram(datagram)
        }
    }

    private fun handleDatagram(buffer: Buffer) {
        val mcStream = MinecraftInputStream(buffer.bytes)

        val header = RakDatagramFlags(mcStream.readByte())
        if(header.userPacket) {
            println("${System.currentTimeMillis()} IN packet ${header.packetId.toByte().toHexStr()}")
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
            println("${System.currentTimeMillis()} IN rak packet ${header.packetId.toByte().toHexStr()}")
            handleRakNetPacket(header, mcStream)
        }
    }

    private fun handleAcknowledge(mcStream: MinecraftInputStream) {
        val ack = AckPePacket.Codec.deserialize(mcStream) as AckPePacket
        val ackedCount = ack.datagramSeqNos.count()
        println("ack $ackedCount datagrams")
    }

    private fun handleNotAcknowledge(mcStream: MinecraftInputStream) {
        println("nack")
    }

    private fun handleConnectedUserPacket(datagramHeader: RakDatagramFlags, mcStream: MinecraftInputStream) {
        val datagramSeqNo = mcStream.read3BytesInt()

        queueAck(datagramSeqNo)

        val messages = ArrayList<ByteArray>(6)
        while(mcStream.available() != 0) {
            val msgHeader = RakMessageFlags(mcStream.readByte())
            val bitsLength = mcStream.readUnsignedShort()
            val messageSize = Math.ceil(bitsLength.toDouble() / 8).toInt()

            val reliability = msgHeader.reliability
            if(reliability.reliable) {
                val messageNo = mcStream.read3BytesInt()
            }
            if(reliability.sequenced) {
                val orderIndex = mcStream.read3BytesInt()
                val orderChannel = mcStream.readByte()
            }

            val message = ByteArray(messageSize)
            
            val hasSplit = msgHeader.hasSplit
            if(hasSplit) {
                val splits = mcStream.readInt()
                val splitsId = mcStream.readShort().toInt()
                val splitIndex = mcStream.readInt()

                mcStream.read(message)
                
                val splitMessage = incompleteSplits.computeIfAbsent(splitsId) { SplittedMessage(splitsId, splits) }
                splitMessage.addSplit(splitIndex, message)
                
                val full = splitMessage.full
                val complete = splitMessage.isComplete
                if(full != null) {
                    println("Completed a split message")
                    messages.add(full)
                } else {
                    println("not full yet! $splits $splitsId $splitIndex $complete")
                }
            }
            else {
                mcStream.read(message)
                messages.add(message)
            }
        }
        println("datagram with ${messages.size} full message")

        messages.forEach { handleMessage(MinecraftInputStream(it)) }
    }

    private fun handleMessage(mcStream: MinecraftInputStream) {
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
                sendConnected(response, RaknetReliability.UNRELIABLE)
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
                sendConnected(response, RaknetReliability.RELIABLE)
            }
            is EncryptionWrapperPePacket -> {
                println("wrapper!")
                if(encrypted) {
                    throw NotImplementedError("Encryption not supported yet!")
                } else {
                    val payloadStream = MinecraftInputStream(message.payload)
                    handleMessage(payloadStream)
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

    fun sendConnected(packet: PePacket, reliability: RaknetReliability = RaknetReliability.RELIABLE_ORDERED) {
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

        val messageHeader = RakMessageFlags(reliability, false)
        dMcStream.writeByte(messageHeader.header)
        dMcStream.writeShort(packetBuffer.size * 8)
        
        if(reliability.reliable)
            dMcStream.write3BytesInt(messageSeqNo++)
        if(reliability.sequenced) {
            dMcStream.write3BytesInt(messageOrderIndex++)
            dMcStream.writeByte(0) // order channel
        }
        dMcStream.write(packetBuffer)

        println("${System.currentTimeMillis()} OUT connected ${packet.javaClass.simpleName} ${packet.id.toByte().toHexStr()}")
        socket.send(Buffer.buffer(dByteStream.toByteArray()), address.port(), address.host()) {}
    }
}