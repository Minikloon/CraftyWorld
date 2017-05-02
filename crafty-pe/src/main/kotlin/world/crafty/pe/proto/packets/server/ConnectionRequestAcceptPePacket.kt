package world.crafty.pe.proto.packets.server

import io.vertx.core.net.SocketAddress
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class ConnectionRequestAcceptPePacket(
        val systemAddress: SocketAddress,
        val systemIndex: Int,
        val systemAddresses: Array<SocketAddress>,
        val incommingTimestamp: Long,
        val serverTimestamp: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x10
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ConnectionRequestAcceptPePacket) throw IllegalArgumentException()
            stream.writeAddress(obj.systemAddress)
            stream.writeShort(obj.systemIndex)
            obj.systemAddresses.forEach { stream.writeAddress(it) }
            stream.writeLong(obj.incommingTimestamp)
            stream.writeLong(obj.serverTimestamp)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ConnectionRequestAcceptPePacket(
                    systemAddress = stream.readAddress(),
                    systemIndex = stream.readInt(),
                    systemAddresses = (1..10).map { stream.readAddress() }.toTypedArray(),
                    incommingTimestamp = stream.readLong(),
                    serverTimestamp = stream.readLong()
            )
        }
    }
}