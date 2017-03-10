package world.crafty.pe

import world.crafty.pe.jwt.LoginExtraData
import world.crafty.pe.raknet.RakMessageReliability.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.net.SocketAddress
import io.vertx.core.net.impl.SocketAddressImpl
import org.joml.Vector2f
import org.joml.Vector3f
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.EvictingQueue
import world.crafty.common.utils.average
import world.crafty.common.utils.kotlin.firstOrCompute
import world.crafty.common.utils.toBytes
import world.crafty.common.utils.toHexStr
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.ServerBoundPeRaknetPackets
import world.crafty.pe.proto.ServerBoundPeTopLevelPackets
import world.crafty.pe.proto.packets.client.*
import world.crafty.pe.proto.packets.mixed.*
import world.crafty.pe.proto.packets.server.*
import world.crafty.pe.raknet.packets.*
import world.crafty.pe.raknet.*
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.Deflater
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class PeNetworkSession(val server: PeConnectionServer, val socket: DatagramSocket, val address: SocketAddress) : AbstractVerticle() {    
    private var datagramSeqNo: Int = 0
    private var messageSeqNo: Int = 0
    private var messageOrderIndex: Int = 0
    private var messageSplitId: Short = 0
    
    private var mtuSize = 1000
    
    private var loggedIn = false
    var loginExtraData: LoginExtraData? = null
        private set

    private var encrypted: Boolean = false
    private lateinit var cipher: Cipher
    private lateinit var decipher: Cipher
    private lateinit var secretKey: SecretKey
    private val sendCounter = AtomicLong(0)
    
    private val lastFewRoundTrips = EvictingQueue<Duration>(16)
    private val roundTripTime: Duration
        get() = if(lastFewRoundTrips.size == 0) Duration.ofMillis(200) else lastFewRoundTrips.average()
    
    private var datagramsPerSendLoop = 16
    private var lastNack = Instant.MIN
    
    private val incompleteSplits = mutableMapOf<Short, SplittedMessage>()
    private val orderingChannels = mutableMapOf<Byte, OrderingChannel>()

    private val receiveQueue = ConcurrentLinkedQueue<Buffer>()
    private val receiveAckBuffer = mutableListOf<Int>()
    private val packetSendQueue = mutableListOf<PePacket>()
    private val datagramSendQueue: Queue<RakDatagram> = LinkedList<RakDatagram>()
    private val needAcks = mutableMapOf<Int, RakSentDatagram>()
    
    override fun start() {
        vertx.setPeriodic(2500) { _ -> pruneSplittedDatagrams() }
        vertx.setPeriodic(10) { _ -> processDatagramReceiveQueue() }
        vertx.setPeriodic(10) { _ -> processDatagramSendQueue() }
        vertx.setPeriodic(20) { _ -> processPacketSendQueue() }
        vertx.setPeriodic(40) { _ -> processResends() }
        vertx.setPeriodic(25) { _ -> processOrderingChannels() }
        vertx.setPeriodic(10) { _ -> flushAckBuffer() }
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
            if(headerFlags.ack) {
                handleRakAcknowledge(mcStream)
            }
            else if(headerFlags.nack) {
                handleRakNotAcknowledge(mcStream)
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

    private fun handleRakAcknowledge(mcStream: MinecraftInputStream) {
        val ack = AckPePacket.Codec.deserialize(mcStream) as AckPePacket
        ack.datagramSeqNos.forEach { 
            val acked = needAcks.remove(it) ?: return@forEach
            lastFewRoundTrips.add(acked.sinceLastSend)
        }
    }

    private fun handleRakNotAcknowledge(mcStream: MinecraftInputStream) {
        println("nack")
        if(Duration.between(Instant.now(), lastNack) > roundTripTime)
            datagramsPerSendLoop = Math.max(1, (datagramsPerSendLoop * 0.8).toInt())
        lastNack = Instant.now()        
    }

    private val supportedReliabilities = setOf(RELIABLE, RELIABLE_ORDERED, UNRELIABLE)
    
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
                    println("Completed a split message")
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
                handlePePayload(it.dataAsStream)
            }
        }
    }
    
    fun processOrderingChannels() {
        orderingChannels.values.forEach { 
            val message = it.poll() ?: return@forEach
            handlePePayload(message.dataAsStream)
        }
    }
    
    private fun handlePePayload(mcStream: MinecraftInputStream) {
        val id = mcStream.read()
        val codec = ServerBoundPeTopLevelPackets.idToCodec[id]
        if(codec == null) {
            println("Unknown pe message id $id")
            return
        }
        val message = codec.deserialize(mcStream)
        handlePeMessage(message)
    }

    private fun handlePeMessage(message: PePacket) {
        if(message !is EncryptionWrapperPePacket && message !is CompressionWrapperPePacket && message !is SetPlayerPositionPePacket)
            println("${System.currentTimeMillis()} HANDLE ${message::class.java.simpleName}")
        when(message) {
            is ConnectedPingPePacket -> {
                val response = ConnectedPongPePacket(message.pingTimestamp, System.currentTimeMillis())
                sendNow(response, UNRELIABLE)
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
                sendNow(response, RELIABLE)
            }
            is EncryptionWrapperPePacket -> {
                if(encrypted) {
                    throw NotImplementedError("Encryption not supported yet!")
                } else {
                    val payloadStream = MinecraftInputStream(message.payload)
                    handlePePayload(payloadStream)
                    if(payloadStream.available() != 0)
                        throw IllegalStateException("at least ${payloadStream.available()} remaining in an encryption wrapper")
                }
            }
            is CompressionWrapperPePacket -> {
                message.packets.forEach { handlePeMessage(it) }
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
                    val handshake = ServerHandshakePePacket(server.keyPair.public, sessionToken)
                    queueSend(handshake)
                }
                else {
                    loggedIn = true
                    queueSend(PlayerStatusPePacket(PlayerStatus.LOGIN_ACCEPTED))
                    queueSend(ResourcePackTriggerPePacket(false, listOf(), listOf()))
                    println("Login from $address ${message.protocolVersion} ${message.edition}")
                }
            }
            is ResourcePackClientResponsePePacket -> {
                println("resource pack response status: ${message.status}")
                val startGame = {
                    queueSend(StartGamePePacket(
                            entityId = 2,
                            runtimeEntityId = 0,
                            spawn = Vector3f(0f, 18f, 0f),
                            yawAndPitch = Vector2f(0f, 0f),
                            seed = 12345,
                            dimension = 0,
                            generator = 1,
                            gamemode = GameMode.SURVIVAL,
                            difficulty = 0,
                            x = 0,
                            y = 18,
                            z = 0,
                            achievementsDisabled = true,
                            dayCycleStopTime = 6000,
                            eduEdition = false,
                            rainLevel = 0f,
                            lightningLevel = 0f,
                            enableCommands = false,
                            resourcePackRequired = false,
                            levelId = "1m0AAMIFIgA=",
                            worldName = "test"
                    ))
                    queueSend(SetChunkRadiusPePacket(5))
                    queueSend(SetAttributesPePacket(0, PlayerAttribute.defaults))
                    /*
                    queueSend(SetTimePePacket(60000, false))
                    queueSend(SetDifficultyPePacket(0))
                    queueSend(SetCommandsEnabledPePacket(false))
                    queueSend(SetAdventureSettingsPePacket(AdventureSettingsFlags(
                            worldImmutable = false,
                            noPvp = false,
                            noPve = false,
                            passiveMobs = false,
                            autoJump = false,
                            allowFly = false,
                            noClip = false
                    ), 0
                     queueSend(SetAttributesPePacket(0, PlayerAttribute.defaults))
                    */
                }
                println("status ${message.status}")
                when(message.status) {
                    ResourcePackClientStatus.REQUEST_DATA -> {
                        startGame()
                    }
                    ResourcePackClientStatus.PLAYER_READY -> {
                        startGame()
                    }
                    else -> {
                        throw NotImplementedError("unhandled resource pack status ${message.status}")
                    }
                }
            }
            is ChunkRadiusRequestPePacket -> {
                println("Requested chunk radius: ${message.desiredChunkRadius}")
                val chunkRadius = 5
                queueSend(SetChunkRadiusPePacket(chunkRadius))
                //sendNow(EncryptionWrapperPePacket(CompressionWrapperPePacket(SetChunkRadiusPePacket(chunkRadius)).serializedWithId()), RELIABLE_ORDERED)
                val squareRange = (-chunkRadius..chunkRadius)
                var sent = 0
                squareRange.forEach { chunkX ->
                    squareRange.forEach { chunkZ ->
                        val bs = ByteArrayOutputStream(1024)
                        val mcStream = MinecraftOutputStream(bs)
                        val chunksPerColumn = 1
                        mcStream.writeByte(chunksPerColumn)
                        (0 until chunksPerColumn).forEach { chunkLayer ->
                            val blocksPerChunk = 16*16*16
                            val blocks = ByteArray(blocksPerChunk) { 0x00 }
                            if(chunkLayer == 0) {
                                (0 until 16).forEach { bx ->
                                    (0 until 16).forEach { bz ->
                                        (0 until 4).forEach { by ->
                                            val idx = (bx * 256) + (bz * 16) + (by)
                                            val type = when (by) {
                                                0 -> 7
                                                1 -> 3
                                                2 -> 3
                                                3 -> 2
                                                else -> 0
                                            }
                                            blocks[idx] = type.toByte()
                                        }
                                    }
                                }
                            }
                            val data = ByteArray(blocksPerChunk / 2) { 0x00 }
                            val blockLight = ByteArray(blocksPerChunk / 2) { 0x00.toByte() }
                            val skyLight = if(chunkLayer == 0) {
                                ByteArray(blocksPerChunk / 2) {
                                    val mod16 = it % 16
                                    if(mod16 == 0 || mod16 == 1 || mod16 == 8 || mod16 == 9) {
                                        0
                                    } else {
                                        0xFF.toByte()
                                    }
                                }
                            }
                            else {
                                ByteArray(blocksPerChunk / 2) { 0x00.toByte() }
                            }
                            mcStream.writeByte(0) // chunk mode
                            mcStream.write(blocks)
                            mcStream.write(data)
                            mcStream.write(skyLight)
                            mcStream.write(blockLight)
                        }
                        val height = ByteArray(256*2) { if(it < 256) 4 else 0 }
                        val biomeIds = ByteArray(16*16) { 0x01 }
                        mcStream.write(height)
                        mcStream.write(biomeIds)
                        mcStream.writeByte(0) // something about border blocks

                        mcStream.writeSignedVarInt(0) // block entities

                        val chunkBytes = bs.toByteArray()
                        
                        val chunkPacket = FullChunkDataPePacket(chunkX, chunkZ, chunkBytes)
                        
                        val batched = CompressionWrapperPePacket(chunkPacket)
                        val compressed = batched.serializedWithId(Deflater.BEST_COMPRESSION)

                        sendNow(EncryptionWrapperPePacket(compressed), RELIABLE_ORDERED)
                        if(sent++ == 56) {
                            queueSend(PlayerStatusPePacket(PlayerStatus.SPAWN))
                        }
                    }
                }
            }
            is PlayerActionPePacket -> {
                println("action ${message.action}")
            }
            is SetPlayerPositionPePacket -> {
                
            }
            else -> {
                println("Unhandled pe message ${message.javaClass.simpleName}")
            }
        }
    }

    private fun handleRakNetPacket(header: RakDatagramFlags, mcStream: MinecraftInputStream) {
        val codec = ServerBoundPeRaknetPackets.idToCodec[header.packetId.toInt()]
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
                val serverId = address.hashCode().toLong()
                val color = if(count % 2 == 0) "§c" else "§4"
                val motdFirstLine = "$color❤ §a§lHELLO WORLD $color❤"
                val motdSecondLine = "§eCONNECT ${count++}"
                val onlinePlayers = 0
                val maxPlayers = 1000
                val serverListData = "MCPE;$motdFirstLine;100;1.0.0.7;$onlinePlayers;$maxPlayers;$serverId;$motdSecondLine;Survival;"
                val reply = UnconnectedPongServerPePacket(packet.pingId, serverId, serverListData)
                sendNowPlain(reply)
            }
            is OpenConnectionRequest1PePacket -> {
                println("open connection request 1 mtu ${packet.mtuSize}")
                mtuSize = packet.mtuSize - 68
                val reply = OpenConnectionReply1PePacket(1234, false, packet.mtuSize)
                sendNowPlain(reply)
            }
            is OpenConnectionRequest2PePacket -> {
                println("open connection request 2")
                val reply = OpenConnectionReply2PePacket(1234, address, packet.mtuSize, false)
                sendNowPlain(reply)
            }
            else -> {
                println("unhandled packet ${packet.javaClass.name}")
            }
        }
    }

    fun queueAck(seqNo: Int) {
        receiveAckBuffer.add(seqNo)
    }
    
    fun flushAckBuffer() {
        if(receiveAckBuffer.isEmpty()) return
        val ack = AckPePacket(receiveAckBuffer)
        sendNowPlain(ack)
        receiveAckBuffer.clear()
    }

    fun queueDatagram(datagram: RakDatagram) {
        datagramSendQueue.add(datagram)
        var sent = needAcks[datagram.sequenceNumber]
        if(sent == null) {
            sent = RakSentDatagram(datagram, this)
            needAcks[datagram.sequenceNumber] = sent
        }
        sent.incSend()
    }
    
    fun processDatagramSendQueue() {
        if(System.currentTimeMillis() % 500 == 0L)
            datagramsPerSendLoop = Math.min(128, datagramsPerSendLoop + 1)
        (1..datagramsPerSendLoop).forEach { 
            val datagram = datagramSendQueue.poll() ?: return
            socket.send(Buffer.buffer(datagram.serialized()), address.port(), address.host()) {}
        }
    }

    fun queueSend(packet: PePacket) {
        require(packet !is CompressionWrapperPePacket) { "can't queue up compression wrappers, send them directly or queue their decompressed content" }
        packetSendQueue.add(packet)
        println("${System.currentTimeMillis()} QUEUE ${packet::class.java.simpleName}")
    }
    
    private fun processPacketSendQueue() {
        val size = packetSendQueue.size
        if(size == 0)
            return
        
        val wrappedPacket = if(size == 1) {
            EncryptionWrapperPePacket(packetSendQueue[0].serializedWithId())
        } else {
            val batched = CompressionWrapperPePacket(packetSendQueue)
            EncryptionWrapperPePacket(batched.serializedWithId())
        }

        sendNow(wrappedPacket, RELIABLE_ORDERED)
        packetSendQueue.clear()
    }

    fun sendNow(packet: PePacket, reliability: RakMessageReliability) {
        val message = RakMessage(
                RakMessageFlags(reliability),
                if(reliability.reliable) RakMessage.MetaReliability(messageSeqNo++) else null,
                if(reliability.ordered) RakMessage.MetaOrder(messageOrderIndex++, channel = 0) else null,
                null,
                packet.serializedWithId()
        )
        val datagrams = binpackMessagesInDatagrams(message)
        datagrams.forEach { queueDatagram(it) }
    }

    private fun binpackMessagesInDatagrams(vararg messages: RakMessage) : List<RakDatagram> {
        val maxSize = mtuSize - RakDatagram.headerSize
        
        val messagesWithSplits = splitLargeMessages(messages, maxSize)
        messagesWithSplits.sortedByDescending { it.size }
        
        val groups = mutableListOf<ByteArrayOutputStream>()
        messagesWithSplits.forEach { message ->
            val group = groups.firstOrCompute(
                    { it.size() + message.size <= maxSize }, 
                    { ByteArrayOutputStream(maxSize) }
            )
            message.serialize(MinecraftOutputStream(group))
        }
        
        return groups.mapIndexed { index, bs -> 
            val headerFlags = if(index == 0) RakDatagramFlags.nonContinuousUserDatagram else RakDatagramFlags.continuousUserDatagram
            RakDatagram(headerFlags, datagramSeqNo++, bs.toByteArray())
        }
    }

    private fun splitLargeMessages(packets: Array<out RakMessage>, maxSize: Int) : List<RakMessage> {
        return packets.flatMap { packet ->
            if(packet.size < maxSize) listOf(packet)
            else {
                val splitId = messageSplitId++
                var splitsCount = Math.ceil((packet.size.toDouble() / maxSize)).toInt()
                val overheadFromSplitting = splitsCount * RakMessage.MetaSplits.size
                if(packet.size + overheadFromSplitting > splitsCount * maxSize) ++splitsCount
                
                (0 until splitsCount).map { splitIndex ->
                    val dataIndex = splitIndex * (maxSize - RakMessage.MetaSplits.size)
                    val splitData = packet.data.copyOfRange(
                            dataIndex,
                            dataIndex + Math.min(maxSize, packet.data.size - dataIndex))
                    RakMessage(
                            RakMessageFlags(packet.headerFlags.reliability, hasSplit = true),
                            RakMessage.MetaReliability(messageSeqNo++),
                            packet.order,
                            RakMessage.MetaSplits(splitsCount, splitId, splitIndex),
                            splitData
                    )
                }
            }
        }
    }

    fun sendNowPlain(packet: PePacket) {
        if(packet !is AckPePacket && packet !is UnconnectedPongServerPePacket)
            println("${System.currentTimeMillis()} OUT unconnected ${packet.javaClass.simpleName} ${packet.id.toByte().toHexStr()}")

        val serialized = packet.serializedWithId()
        socket.send(Buffer.buffer(serialized), address.port(), address.host()) {}
    }
    
    /*
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
    */
    
    private fun createTrailer(payload: ByteArray, senderCounter: Long, secretKey: SecretKey) : ByteArray {
        val sha256 = MessageDigest.getInstance("SHA-256")
        sha256.update(senderCounter.toBytes())
        sha256.update(payload)
        sha256.update(secretKey.encoded)
        return sha256.digest()
    }

    fun processResends() {
        val ackTimeoutMs = Math.max(10, roundTripTime.toMillis() * 2)
        needAcks.values.forEach { sentDatagram ->
            if(sentDatagram.sinceLastSend.toMillis() > ackTimeoutMs) {
                queueDatagram(sentDatagram.datagram)
            }
        }
    }
    
    companion object {
        private val mojangPubKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(
                "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V"
        )))
    }
}