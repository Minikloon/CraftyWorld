package world.crafty.pe

import world.crafty.pe.raknet.RakMessageReliability.*
import io.vertx.core.Vertx
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.eventbus.EventBus
import io.vertx.core.net.SocketAddress
import kotlinx.coroutines.experimental.launch
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.utils.*
import world.crafty.common.vertx.*
import world.crafty.pe.entity.PeEntity
import world.crafty.pe.proto.*
import world.crafty.pe.proto.packets.client.*
import world.crafty.pe.proto.packets.mixed.*
import world.crafty.pe.proto.packets.server.*
import world.crafty.pe.raknet.*
import world.crafty.pe.raknet.packets.DisconnectNotifyPePacket
import world.crafty.pe.raknet.session.RakNetworkSession
import world.crafty.pe.session.PeSessionState
import world.crafty.pe.session.pass.EncryptionPass
import world.crafty.pe.session.pass.NoEncryptionPass
import world.crafty.pe.session.states.ConnectionPeSessionState
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.*

private val log = logger<PeNetworkSession>()
class PeNetworkSession(val server: PeConnectionServer, val worldServer: String, socket: DatagramSocket, address: SocketAddress) : RakNetworkSession(socket, address) {
    private lateinit var state: PeSessionState
    
    var encryptionPass: EncryptionPass = NoEncryptionPass
    
    private val packetSendQueue = mutableListOf<PePacket>()
    
    private lateinit var eb: EventBus
    
    private var lastPing = Instant.now()

    override fun onStart(vertx: Vertx) {
        state = ConnectionPeSessionState(this)
        eb = vertx.eventBus()
        vertx.setPeriodic(10) { _ -> processPacketSendQueue() }
        vertx.setPeriodic(5000) {
            if(lastPing.sinceThen() > Duration.ofMillis(5500))
                disconnect("Timeout")
        }
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
        val packet = codec.deserialize(payload)
        launch(CurrentVertx) {
            try {
                handlePeMessage(packet)
            } catch(e: Exception) {
                log.error(e) { "Error while handling packet ${packet::class.simpleName}" }
                //close()
            }
        }
    }
    
    private suspend fun handlePeMessage(message: PePacket) {
        if(message !is EncryptionWrapperPePacket && message !is CompressionWrapperPePacket && message !is SetPlayerLocPePacket && message !is ConnectedPingPePacket)
            log.trace { "HANDLE ${message::class.simpleName}" }
        when(message) {
            is ConnectedPingPePacket -> {
                val response = ConnectedPongPePacket(message.pingTimestamp, System.currentTimeMillis())
                lastPing = Instant.now()
                log.info { "Ping from client ${deploymentID()}!" }
                send(response, UNRELIABLE)
            }
            is EncryptionWrapperPePacket -> {
                val payload = encryptionPass.decrypt(message.payload)
                val payloadStream = MinecraftInputStream(payload)
                onPayload(payloadStream)
                if(payloadStream.available() != 0)
                    throw IllegalStateException("at least ${payloadStream.available()} remaining in an encryption wrapper")
            }
            is CompressionWrapperPePacket -> {
                message.packets.forEach { handlePeMessage(it) }
            }
            is DisconnectNotifyPePacket -> {
                disconnect("Client disconnected")
            }
            else -> {
                state.handle(message)
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

        val packetToEncrypt = if(size == 1) {
            packetSendQueue[0]
        } else { 
            CompressionWrapperPePacket(packetSendQueue)
        }
        
        val rawPayload = packetToEncrypt.serializedWithId()
        val encrypted = encryptionPass.encrypt(rawPayload)
        val wrapped = EncryptionWrapperPePacket(encrypted)

        send(wrapped, RakMessageReliability.RELIABLE_ORDERED)
        packetSendQueue.clear()
    }
    
    suspend fun switchState(newState: PeSessionState) {
        state.stop()
        state = newState
        newState.start()
    }

    override fun onRaknetDisconnect() {
        disconnect("RakNet disconnect")
    }
    
    private var disconnected = false
    fun disconnect(message: String = "Unexpected server disconnect") {
        if(disconnected)
            return
        disconnected = true
        
        send(EncryptionWrapperPePacket(DisconnectPePacket(message)), RELIABLE, immediate = true)
        launch(CurrentVertx) {
            state.onDisconnect(message)
        }

        server.removeSessionSocket(address)

        launch(CurrentVertx) {
            state.stop()
        }

        vertx.undeploy(deploymentID())
    }
    
    companion object {
        val mojangPubKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(
                "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V"
        )))
    }
}