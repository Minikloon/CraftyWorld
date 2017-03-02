package club.kazza.kazzacraft.network

import club.kazza.kazzacraft.network.protocol.*
import club.kazza.kazzacraft.network.protocol.jwt.LoginExtraData
import club.kazza.kazzacraft.network.raknet.*
import club.kazza.kazzacraft.network.raknet.RakMessageReliability.*
import club.kazza.kazzacraft.network.security.generateBytes
import club.kazza.kazzacraft.network.security.isCertChainValid
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import club.kazza.kazzacraft.utils.toBytes
import club.kazza.kazzacraft.utils.toHexStr
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.net.SocketAddress
import io.vertx.core.net.impl.SocketAddressImpl
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class PeNetworkSession(val server: PeConnectionServer, val socket: DatagramSocket, val address: SocketAddress) : AbstractVerticle() {
    private val receiveQueue = ConcurrentLinkedQueue<Buffer>()
    private var datagramSeqNo: Int = 0
    private var messageSeqNo: Int = 0
    private var messageOrderIndex: Int = 0
    
    private var loggedIn = false
    var loginExtraData: LoginExtraData? = null
        private set

    private var encrypted: Boolean = false
    private lateinit var cipher: Cipher
    private lateinit var decipher: Cipher
    private lateinit var secretKey: SecretKey
    private val sendCounter = AtomicLong(0)
    
    private val incompleteSplits = mutableMapOf<Short, SplittedMessage>()
    private val orderingChannels = mutableMapOf<Byte, OrderingChannel>()
    
    private val ackQueue = mutableListOf<Int>()
    
    override fun start() {
        vertx.setPeriodic(2500) { _ -> pruneSplittedDatagrams() }
        vertx.setPeriodic(10) { _ -> processDatagramReceiveQueue() }
        vertx.setPeriodic(25) { _ -> processOrderingChannels() }
        vertx.setPeriodic(10) { _ -> flushAckQueue() }
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
                processConnectedDatagram(datagram)
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
    
    private fun processConnectedDatagram(datagram: RakConnectedDatagram) {
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
                    incompleteSplits.remove(splitsId)
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
        val codec = ServerBoundPePackets.idToCodec[id]
        if(codec == null) {
            println("Unknown pe message id $id")
            return
        }

        val message = codec.deserialize(mcStream)
        when(message) {
            is ConnectedPingPePacket -> {
                val response = ConnectedPongPePacket(message.pingTimestamp, System.currentTimeMillis())
                sendConnected(response, UNRELIABLE, false)
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
                    if(payloadStream.available() != 0)
                        throw IllegalStateException("at least ${payloadStream.available()} remaining in an encryption wrapper")
                }
            }
            is LoginPePacket -> {
                val chain = message.certChain
                val rootCert = if(chain[0].payload.iss == "RealmsAuthorization") mojangPubKey else null
                if(! isCertChainValid(chain, rootCert))
                    throw NotImplementedError("Should do something about invalid cert chain")
                
                if(server.supportsEncryption) { // TODO encrypt if xuid exists
                    val claims = chain.last().payload
                    loginExtraData = claims.extraData

                    val clientKey = claims.idPubKey

                    val agreement = KeyAgreement.getInstance("ECDH")
                    agreement.init(server.keyPair.private)
                    agreement.doPhase(clientKey, true)
                    val sharedSecret = agreement.generateSecret()

                    val sessionToken = generateBytes(128)

                    val sha256 = MessageDigest.getInstance("SHA-256")
                    sha256.update(sessionToken)
                    val secretKeyBytes = sha256.digest(sharedSecret)
                    secretKey = SecretKeySpec(secretKeyBytes, "AES")

                    val symmetricConfig = "AES/CFB8/NoPadding"
                    val iv = Arrays.copyOf(secretKeyBytes, 16)
                    cipher = Cipher.getInstance(symmetricConfig)
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
                    decipher = Cipher.getInstance(symmetricConfig)
                    decipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                    encrypted = true
                    val handshake = ServerToClientHandshakePePacket(server.keyPair.public, sessionToken)
                    sendConnected(handshake)
                }
                else {
                    loggedIn = true
                    val wrapper = CompressionWrapperPePacket(
                            PlayerStatusPePacket(PlayerStatus.LOGIN_ACCEPTED),
                            ResourcePackTriggerPePacket(false, listOf(), listOf())
                    )
                    sendConnected(wrapper)
                    println("Login from $address ${message.protocolVersion} ${message.edition}")
                }
                
            }
            else -> {
                println("Unhandled pe message ${message.javaClass.simpleName}")
            }
        }
    }

    private fun handleRakNetPacket(header: RakDatagramFlags, mcStream: MinecraftInputStream) {
        val codec = ServerBoundPeRaknetPackets.idToCodec[header.packetId]
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
        ackQueue.add(seqNo)
    }
    
    fun flushAckQueue() {
        if(ackQueue.isEmpty()) return
        val ack = AckPePacket(ackQueue)
        sendUnconnected(ack)
        ackQueue.clear()
    }

    fun sendUnconnected(packet: PePacket) {
        val byteStream = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(byteStream)
        mcStream.writeByte(packet.id)
        packet.serialize(mcStream)
        
        println("${System.currentTimeMillis()} OUT unconnected ${packet.javaClass.simpleName} ${packet.id.toByte().toHexStr()}")
        socket.send(Buffer.buffer(byteStream.toByteArray()), address.port(), address.host()) {}
    }

    fun sendConnected(packet: PePacket, reliability: RakMessageReliability = RELIABLE_ORDERED, encryptionWrap: Boolean = true) {
        // TODO support raknet multi-message datagram && multi-datagram message
        val byteStream = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(byteStream)
        if(loggedIn && encryptionWrap)
            mcStream.writeByte(EncryptionWrapperPePacket.Codec.id)
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
    
    fun sendEncrypted(packet: PePacket) {
        val byteStream = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(byteStream)
        mcStream.writeByte(packet.id)
        packet.serialize(mcStream)
        val payload = byteStream.toByteArray()
        mcStream.write(createTrailer(payload, sendCounter.getAndIncrement(), secretKey))
        val payloadAndTrailer = byteStream.toByteArray()
        
        val encryptedPacket = cipher.doFinal(payloadAndTrailer)
        
        val wrapperByteStream = ByteArrayOutputStream()
        val wrapperMcStream = MinecraftOutputStream(wrapperByteStream)
        wrapperMcStream.writeByte(EncryptionWrapperPePacket.Codec.id)
        wrapperMcStream.write(encryptedPacket)
        
        val dByteStream = ByteArrayOutputStream()
        val dMcStream = MinecraftOutputStream(dByteStream)

        dMcStream.writeByte(RakDatagramFlags.nonContinuousUserDatagram)
        dMcStream.write3BytesInt(datagramSeqNo++)
        
        val reliability = RakMessageReliability.RELIABLE_ORDERED

        val message = RakMessage(
                RakMessageFlags(reliability, hasSplit = false),
                if(reliability.reliable) RakMessage.MetaReliability(messageSeqNo++) else null,
                if(reliability.ordered) RakMessage.MetaOrder(messageOrderIndex++, channel = 0) else null,
                null,
                wrapperByteStream.toByteArray()
        )
        message.serialize(dMcStream)

        println("${System.currentTimeMillis()} OUT encrypted+connected ${packet.javaClass.simpleName} ${packet.id.toByte().toHexStr()}")
        socket.send(Buffer.buffer(dByteStream.toByteArray()), address.port(), address.host()) {}
    }
    
    private fun createTrailer(payload: ByteArray, senderCounter: Long, secretKey: SecretKey) : ByteArray {
        val sha256 = MessageDigest.getInstance("SHA-256")
        sha256.update(senderCounter.toBytes())
        sha256.update(payload)
        sha256.update(secretKey.encoded)
        return sha256.digest()
    }
    
    companion object {
        private val mojangPubKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(
                "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V"
        )))
    }
}