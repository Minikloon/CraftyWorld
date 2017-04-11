package world.crafty.pe

import world.crafty.pe.jwt.payloads.LoginExtraData
import world.crafty.pe.raknet.RakMessageReliability.*
import io.vertx.core.Vertx
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.eventbus.EventBus
import io.vertx.core.net.SocketAddress
import io.vertx.core.net.impl.SocketAddressImpl
import kotlinx.coroutines.experimental.launch
import org.joml.Vector3f
import world.crafty.common.Angle256
import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.*
import world.crafty.common.vertx.*
import world.crafty.pe.jwt.payloads.CertChainLink
import world.crafty.pe.jwt.payloads.PeClientData
import world.crafty.pe.entity.PeEntity
import world.crafty.pe.entity.PePlayerEntity
import world.crafty.pe.metadata.PeMetadataMap
import world.crafty.pe.proto.*
import world.crafty.pe.proto.packets.client.*
import world.crafty.pe.proto.packets.mixed.*
import world.crafty.pe.proto.packets.server.*
import world.crafty.pe.raknet.*
import world.crafty.pe.raknet.session.RakNetworkSession
import world.crafty.pe.world.toPePacket
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.CraftyPacket
import world.crafty.proto.CraftySkin
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.packets.client.*
import world.crafty.proto.packets.server.*
import world.crafty.proto.packets.server.ChunkCacheStategy.*
import world.crafty.skinpool.CraftySkinPoolServer
import world.crafty.skinpool.protocol.client.HashPollPoolPacket
import world.crafty.skinpool.protocol.client.SaveSkinPoolPacket
import world.crafty.skinpool.protocol.server.HashPollReplyPoolPacket
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val log = getLogger<PeNetworkSession>()
class PeNetworkSession(val server: PeConnectionServer, val worldServer: String, socket: DatagramSocket, address: SocketAddress) : RakNetworkSession(socket, address) {
    private var loggedIn = false
    var loginExtraData: LoginExtraData? = null // TODO: split the session by login stages to avoid these shits
        private set
    var loginClientData: PeClientData? = null
        private set
    var craftySkin: CraftySkin? = null
        private set
    
    private var craftyPlayerId = 0

    private var encrypted: Boolean = false
    private lateinit var cipher: Cipher
    private lateinit var decipher: Cipher
    private lateinit var secretKey: SecretKey
    private val sendCounter = AtomicLong(0)
    
    private val packetSendQueue = mutableListOf<PePacket>()
    
    private var ownEntityId: Long = 0
    private var clientLoc = PeLocation(0f, 0f, 0f)
    private val loadedEntities = mutableMapOf<Long, PeEntity>()
    
    private lateinit var eb: EventBus

    override fun onStart(vertx: Vertx) {
        eb = vertx.eventBus()
        vertx.setPeriodic(10) { _ -> processPacketSendQueue() }
    }

    override fun getUnconnectedPongExtraData(serverId: Long): ByteArray {
        val color = "§c"
        val extra = ServerListPongExtraData(
                motdFirstLine = "$color❤ §a§lHELLO WORLD $color❤",
                motdSecondLine = "§eCONNECT",
                version = "1.0.0.7",
                onlinePlayers = 0,
                maxPlayers = 1000,
                serverId = serverId
        )
        return extra.serialized()
    }

    override fun onPayload(payload: MinecraftInputStream) {
        val id = payload.read()
        val codec = ServerBoundPeTopLevelPackets.idToCodec[id]
        if(codec == null) {
            log.error { "Unknown pe message id $id" }
            return
        }
        val message = codec.deserialize(payload)
        launch(CurrentVertx) {
            handlePeMessage(message)
        }
    }
    
    private suspend fun handlePeMessage(message: PePacket) {
        if(message !is EncryptionWrapperPePacket && message !is CompressionWrapperPePacket && message !is SetPlayerLocPePacket && message !is ConnectedPingPePacket)
            log.trace { "HANDLE ${message::class.simpleName}" }
        when(message) {
            is ConnectedPingPePacket -> {
                val response = ConnectedPongPePacket(message.pingTimestamp, System.currentTimeMillis())
                send(response, UNRELIABLE)
            }
            is NewIncomingConnection -> {
                log.info { "A client at ${address.host()} is now officially connected!" }
            }
            is ConnectionRequestPePacket -> {
                val response = ConnectionRequestAcceptPePacket(
                        systemAddress = SocketAddressImpl(19132, "127.0.0.1"),
                        systemIndex = 0,
                        systemAddresses = Array(10) { if(it == 0) SocketAddressImpl(19132, "0.0.0.0") else SocketAddressImpl(19132, "127.0.0.1") },
                        incommingTimestamp = message.timestamp,
                        serverTimestamp = System.currentTimeMillis()
                )
                send(response, RELIABLE)
            }
            is EncryptionWrapperPePacket -> {
                if(encrypted) {
                    throw NotImplementedError("Encryption not supported yet!")
                } else {
                    val payloadStream = MinecraftInputStream(message.payload)
                    onPayload(payloadStream)
                    if(payloadStream.available() != 0)
                        throw IllegalStateException("at least ${payloadStream.available()} remaining in an encryption wrapper")
                }
            }
            is CompressionWrapperPePacket -> {
                message.packets.forEach { handlePeMessage(it) }
            }
            is LoginPePacket -> {
                val chain = message.certChain
                
                val firstChainLink = chain[0].payload as CertChainLink
                if(firstChainLink.certificateAuthority && firstChainLink.idPubKey != mojangPubKey) {
                    throw Exception("Login first chain link claims to be ca but doesn't hold the mojang pub key")
                }
                
                if(! isCertChainValid(chain))
                    throw NotImplementedError("Should do something about invalid cert chain")

                val lastClaim = chain.last().payload as CertChainLink
                val clientKey = lastClaim.idPubKey
                loginExtraData = lastClaim.extraData
                
                val clientDataJwt = message.clientData
                if(clientDataJwt.header.x5uKey != clientKey || !clientDataJwt.isSignatureValid) {
                    throw NotImplementedError("Should do something about invalid client data signature")
                }
                
                val clientData = clientDataJwt.payload as PeClientData
                loginClientData = clientData
                val png = clientData.skinPng
                val skinHash = hashFnv1a64(png)
                craftySkin = CraftySkin(skinHash, png)
                eb.typedSend<HashPollReplyPoolPacket>(CraftySkinPoolServer.channelPrefix, HashPollPoolPacket(skinHash, false)) {
                    if(it.failed()) {
                        it.cause().printStackTrace()
                        return@typedSend
                    }
                    val res = it.result()
                    if(! res.body().hasProfile) {
                        val slim = isSlim(clientData.skinData)
                        val skinPng = clientData.skinPng
                        eb.typedSend(CraftySkinPoolServer.channelPrefix, SaveSkinPoolPacket(skinHash, slim, skinPng))
                    }
                }
                
                if(server.supportsEncryption) { // TODO encrypt if xuid exists
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
                    log.info { "Login from $address ${message.protocolVersion} ${message.edition}" }
                }
            }
            is ResourcePackClientResponsePePacket -> {
                val startGame: suspend () -> Unit = {
                    val username = loginExtraData?.displayName ?: throw IllegalStateException("ResourcePackClientResponse without prior login!")
                    val skin = craftySkin ?: throw IllegalStateException("ResourcePackClientResponse without skin!")
                    val craftyResponse = vxm<JoinResponseCraftyPacket> { eb.send("$worldServer:join", JoinRequestCraftyPacket(username, true, false, MinecraftPlatform.PE, skin), it) }
                    if (!craftyResponse.accepted)
                        throw IllegalStateException("CraftyServer denied our join request :(")

                    val prespawn = craftyResponse.prespawn!!
                    ownEntityId = prespawn.entityId
                    clientLoc = PeLocation(prespawn.spawnLocation)
                    queueSend(StartGamePePacket(
                            entityId = prespawn.entityId,
                            runtimeEntityId = 0,
                            spawn = PeLocation(prespawn.spawnLocation),
                            seed = 12345,
                            dimension = 0,
                            generator = 1,
                            gamemode = prespawn.gamemode,
                            difficulty = 0,
                            x = 0,
                            y = 50,
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
                    
                    craftyPlayerId = craftyResponse.playerId!!

                    eb.consumer<ChatMessageCraftyPacket>("p:c:$craftyPlayerId:chat") { // TODO: move this somewhere more sensible
                        val text = it.body().text
                        queueSend(ChatPePacket(ChatType.CHAT, "", text))
                    }
                    
                    queueSend(SetChunkRadiusPePacket(5))
                    queueSend(SetAttributesPePacket(0, PlayerAttribute.defaults))
                }
                
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
                val chunkRadius = Math.min(5, message.desiredChunkRadius)
                queueSend(SetChunkRadiusPePacket(chunkRadius))

                eb.typedConsumer("p:c:$craftyPlayerId", AddPlayerCraftyPacket::class) {
                    val packet = it.body()

                    val entity = PePlayerEntity(packet.entityId)
                    loadedEntities[entity.id] = entity
                    
                    if(packet.craftyId == craftyPlayerId)
                        return@typedConsumer
                    
                    val metadata = entity.metaFromCrafty(server.metaTranslatorRegistry, packet.meta)
                    
                    queueSend(PlayerListItemPePacket(
                            action = PlayerListAction.ADD,
                            items = listOf(
                                    PlayerListPeAdd(
                                            uuid = packet.uuid,
                                            entityId = packet.entityId,
                                            name = packet.username,
                                            skin = PeSkin.fromCrafty(packet.skin)
                                    )
                            )
                    ))
                    queueSend(AddPlayerPePacket(
                            uuid = packet.uuid,
                            username = packet.username,
                            entityId = packet.entityId,
                            runtimeEntityId = packet.entityId,
                            x = packet.location.x,
                            y = packet.location.y,
                            z = packet.location.z,
                            speedX = 0f,
                            speedY = 0f,
                            speedZ = 0f,
                            headPitch = packet.location.headPitch.toDegrees(),
                            headYaw = packet.location.bodyYaw.toDegrees(),
                            bodyYaw = packet.location.bodyYaw.toDegrees(),
                            itemInHand = PeItem(0, 0, 0, null),
                            metadata = metadata
                    ))
                    queueSend(PlayerListItemPePacket(PlayerListAction.REMOVE, listOf(PlayerListPeRemove(packet.uuid))))
                }
                
                eb.typedConsumer("p:c:$craftyPlayerId", PatchEntityCraftyPacket::class) {
                    val packet = it.body()
                    val entity = loadedEntities[packet.entityId] ?: return@typedConsumer
                    val meta = entity.metaFromCrafty(server.metaTranslatorRegistry, packet.values)
                    queueSend(EntityMetadataPePacket(packet.entityId, meta))
                }
                
                eb.typedConsumer("p:c:$craftyPlayerId", SetEntityLocationCraftyPacket::class) {
                    val packet = it.body()
                    if(packet.entityId == ownEntityId) return@typedConsumer
                    
                    val entity = loadedEntities[packet.entityId] ?: return@typedConsumer
                    
                    queueSend(entity.getSetLocationPacket(packet.location.toPe(), packet.onGround))
                }
                
                eb.typedConsumer("p:c:$craftyPlayerId", SpawnSelfCraftyPacket::class) {
                    queueSend(PlayerStatusPePacket(PlayerStatus.SPAWN))
                }
                
                val cache = server.getWorldCache(worldServer)
                eb.typedSend<ChunksRadiusResponseCraftyPacket>("p:s:$craftyPlayerId", ChunksRadiusRequestCraftyPacket(clientLoc.positionVec3(), 0, chunkRadius)) {
                    val response = it.result().body()
                    response.chunkColumns.forEach { setColumn ->
                        val chunkPacket = when(setColumn.cacheStrategy) {
                            PER_WORLD -> cache.getOrComputeChunkPacket(setColumn, CraftyChunkColumn::toPePacket)
                            PER_PLAYER -> setColumn.chunkColumn.toPePacket()
                        }
                        send(chunkPacket, RELIABLE_ORDERED)
                    }
                    log.debug { "sent pe ${response.chunkColumns.size} chunks" }
                    
                    eb.typedSend("p:s:$craftyPlayerId", ReadyToSpawnCraftyPacket())
                }
            }
            is PlayerActionPePacket -> {                
                val action = message.action
                val craftyAction = when(action) {
                    PePlayerAction.START_BREAK -> return
                    PePlayerAction.ABORT_BREAK -> return
                    PePlayerAction.STOP_BREAK -> return
                    PePlayerAction.UNKNOWN_3 -> return
                    PePlayerAction.UNKNOWN_4 -> return
                    PePlayerAction.RELEASE_ITEM -> PlayerAction.DROP_ITEM
                    PePlayerAction.STOP_SLEEPING -> PlayerAction.LEAVE_BED
                    PePlayerAction.RESPAWN -> return
                    PePlayerAction.JUMP -> return
                    PePlayerAction.START_SPRINT -> PlayerAction.START_SPRINT
                    PePlayerAction.STOP_SPRINT -> PlayerAction.STOP_SPRINT
                    PePlayerAction.START_SNEAK -> PlayerAction.START_SNEAK
                    PePlayerAction.STOP_SNEAK -> PlayerAction.STOP_SNEAK
                    PePlayerAction.START_DIMENSION_CHANGE -> return
                    PePlayerAction.ABORT_DIMENSION_CHANGE -> return
                    PePlayerAction.START_GLIDE -> return
                    PePlayerAction.STOP_GLIDE -> return
                }
                eb.typedSend("p:s:$craftyPlayerId", PlayerActionCraftyPacket(craftyAction))
            }
            is SetPlayerLocPePacket -> {
                if(message.mode != MoveMode.INTERPOLATE) {
                    log.warn { "Received PE move of mode ${message.mode} from ${loginExtraData?.displayName}" }
                    return
                }
                val newLoc = message.loc.copy(y = message.loc.y - PePlayerEntity.eyeHeight)
                
                val posChanged = newLoc.x != clientLoc.x || newLoc.y != clientLoc.y || newLoc.z != clientLoc.z
                val lookChanged = newLoc.bodyYaw != clientLoc.bodyYaw || newLoc.headYaw != clientLoc.headYaw || newLoc.headPitch != clientLoc.headPitch
                
                val packet: CraftyPacket = if(!posChanged && !lookChanged) { return }
                else if(posChanged && !lookChanged) {
                    SetPlayerPosCraftyPacket(Vector3f(newLoc.x, newLoc.y, newLoc.z), message.onGround)                   
                } else if(posChanged && lookChanged) {
                    SetPlayerPosAndLookCraftyPacket(newLoc.toLocation(), message.onGround)
                } else {
                    SetPlayerLookCraftyPacket(headPitch = newLoc.headPitch, headYaw = newLoc.headYaw, bodyYaw = newLoc.bodyYaw)
                }

                eb.typedSend("p:s:$craftyPlayerId", packet)
                
                clientLoc = newLoc
            }
            is ChatPePacket -> {
                eb.typedSend("p:s:$craftyPlayerId", ChatFromClientCraftyPacket(message.text))
            }
            else -> {
                log.warn { "Unhandled pe message ${message.javaClass.simpleName}" }
            }
        }
    }

    fun queueSend(packet: PePacket) {
        require(packet !is CompressionWrapperPePacket) { "can't queue up compression wrappers, send them directly or queue their decompressed content" }
        packetSendQueue.add(packet)
        log.trace { "${System.currentTimeMillis()} QUEUE ${packet::class.java.simpleName}" }
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

        send(wrappedPacket, RakMessageReliability.RELIABLE_ORDERED)
        packetSendQueue.clear()
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
        
        ...
    }
    
    private fun createTrailer(payload: ByteArray, senderCounter: Long, secretKey: SecretKey) : ByteArray {
        val sha256 = MessageDigest.getInstance("SHA-256")
        sha256.update(senderCounter.toBytes())
        sha256.update(payload)
        sha256.update(secretKey.encoded)
        return sha256.digest()
    }
    */
    
    companion object {
        private val mojangPubKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(
                "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V"
        )))
    }
}