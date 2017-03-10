package world.crafty.pe.raknet.packets

import io.vertx.core.net.SocketAddress
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.raknet.unconnectedBlabber

class OpenConnectionReply2PePacket(
        val serverId: Long,
        val clientEndpoint: SocketAddress,
        val mtuSize: Int,
        val secured: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x08
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is OpenConnectionReply2PePacket) throw IllegalArgumentException()
            stream.write(unconnectedBlabber)
            stream.writeLong(obj.serverId)
            stream.writeAddress(obj.clientEndpoint)
            stream.writeShort(obj.mtuSize)
            stream.writeBoolean(obj.secured)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            stream.skipBytes(unconnectedBlabber.size)
            return OpenConnectionReply2PePacket(
                    serverId = stream.readLong(),
                    clientEndpoint = stream.readAddress(),
                    mtuSize = stream.readShort().toInt(),
                    secured = stream.readBoolean()
            )
        }
    }
}