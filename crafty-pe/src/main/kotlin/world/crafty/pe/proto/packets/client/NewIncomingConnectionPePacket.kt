package world.crafty.pe.proto.packets.client

import io.vertx.core.net.SocketAddress
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class NewIncomingConnection(
        val clientEndpoint: SocketAddress,
        val addresses: List<SocketAddress>,
        val incomingTimestamp: Long,
        val serverTimestamp: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x13
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is NewIncomingConnection) throw IllegalArgumentException()
            stream.writeAddress(obj.clientEndpoint)
            obj.addresses.forEach { stream.writeAddress(it) }
            stream.writeLong(obj.incomingTimestamp)
            stream.writeLong(obj.serverTimestamp)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return NewIncomingConnection(
                    clientEndpoint = stream.readAddress(),
                    addresses = (1..10).map { stream.readAddress() },
                    incomingTimestamp = stream.readLong(),
                    serverTimestamp = stream.readLong()
            )
        }
    }
}