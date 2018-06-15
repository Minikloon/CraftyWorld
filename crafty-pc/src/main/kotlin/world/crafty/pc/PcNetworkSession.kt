package world.crafty.pc

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.net.NetSocket
import kotlinx.coroutines.experimental.launch
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.pc.proto.*
import world.crafty.pc.proto.packets.client.*
import world.crafty.common.utils.*
import world.crafty.common.vertx.*
import world.crafty.mojang.MojangProfile
import world.crafty.pc.proto.packets.server.DisconnectPcPacket
import world.crafty.pc.session.states.HandshakePcSessionState
import world.crafty.pc.session.pass.NoEncryptionPass
import world.crafty.pc.session.PcSessionState
import world.crafty.pc.session.pass.CompressionPass
import world.crafty.pc.session.pass.EncryptionPass
import world.crafty.pc.session.pass.NoCompressionPass
import world.crafty.proto.CraftyPacket
import world.crafty.proto.packets.client.QuitCraftyPacket
import kotlin.reflect.KClass

private val log = logger<PcNetworkSession>()
class PcNetworkSession(val connServer: PcConnectionServer, val worldServer: String, private val socket: NetSocket) {
    val address = socket.remoteAddress().host()

    public var state: PcSessionState = HandshakePcSessionState(this) // TODO: oof
    
    var encryptionPass: EncryptionPass = NoEncryptionPass
    var compressionPass: CompressionPass = NoCompressionPass
    
    suspend fun switchState(newState: PcSessionState) {
        state.stop()
        state = newState
        newState.start()
    }

    fun send(content: LengthPrefixedContent) {
        val buffer = Buffer.buffer(content.expectedSize)
        val bs = BufferOutputStream(buffer)
        val stream = encryptionPass.encryptionStream(bs)
        content.serializeWithLengthPrefix(stream, compressionPass.compressing, compressionPass.threshold)

        try {
            socket.write(buffer)
        } catch(e: Exception) {
            log.debug { "Error sending ${content::class.simpleName} to $address" }
            disconnect()
        }
    }

    fun receive(buffer: Buffer) {
        val raw = buffer.bytes
        val decrypted = encryptionPass.decrypt(raw)
        streamHandler.handle(Buffer.buffer(decrypted))
    }
    
    private val streamHandler = LengthPrefixedHandler {
        val stream = MinecraftInputStream(it.bytes)
        val decompressed = compressionPass.decompressedStream(stream)
        launch(CurrentVertx) {
            handlePayload(MinecraftInputStream(decompressed))
        }
    }
    
    suspend private fun handlePayload(stream: MinecraftInputStream) {
        val packetId = stream.readSignedVarInt()

        val codec = state.packetList.idToCodec[packetId]
        if(codec == null) {
            log.error { "${System.currentTimeMillis()} Unknown pc packet id $packetId while in state $state" }
            return
        }
        val packet = codec.deserialize(stream)
        
        try {
            state.handle(packet)
        } catch(e: Exception) {
            log.error("Error while handling packet ${packet::class.simpleName} within state ${state::class.simpleName}", e)
            disconnect("Connection error")
        }
    }
    
    private var disconnected = false
    fun disconnect(message: String = "Unexpected server shutdown") {
        if(disconnected)
            return
        disconnected = true
        
        launch(CurrentVertx) {
            state.onDisconnect(message)
        }
        
        connServer.removeSessionSocket(socket)

        launch(CurrentVertx) {
            state.stop()
        }
    }
}