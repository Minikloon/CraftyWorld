package world.crafty.pe.raknet.session

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramPacket
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.net.SocketAddress
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.raknet.RakMessageReliability
import world.crafty.pe.raknet.packets.AckPePacket

abstract class RakNetworkSession(val socket: DatagramSocket, val address: SocketAddress) : AbstractVerticle() {
    private val receiver = RakReceiver(this, this::onAck, this::onNack, this::onPayload)
    private val sender = RakSender(this)
    
    var mtuSize = 1000

    override fun start() {
        receiver.registerWithVertx(vertx)
        sender.registerWithVertx(vertx)
        onStart(vertx)
    }
    
    abstract protected fun onStart(vertx: Vertx)

    abstract fun getUnconnectedPongExtraData(serverId: Long) : ByteArray
    
    abstract protected fun onPayload(payload: MinecraftInputStream)

    fun queueReceivedDatagram(datagram: DatagramPacket) {
        receiver.onReceiveDatagram(datagram)
    }

    private fun onAck(ack: AckPePacket) {
        ack.datagramSeqNos.forEach { seqNo ->
            sender.acknowledgeDatagram(seqNo)
        }
    }

    private fun onNack() {
        sender.notifyNotAcknowledge()
    }
    
    fun send(packet: PePacket, reliability: RakMessageReliability) {
        sender.sendPayload(packet.serializedWithId(), reliability)
    }
    
    fun sendRaw(packet: PePacket) {
        sendRaw(packet.serializedWithId())
    }
    
    fun sendRaw(bytes: ByteArray) {
        socket.send(Buffer.buffer(bytes), address.port(), address.host()) {}
    }
}