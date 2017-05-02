package world.crafty.pe.raknet.packets

import io.vertx.core.net.SocketAddress
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.raknet.unconnectedBlabber

class OpenConnectionRequest2PePacket(
        val remoteBindingAddress: SocketAddress,
        val mtuSize: Int,
        val clientUuid: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec

    object Codec : PePacketCodec() {
        override val id = 0x07
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is OpenConnectionRequest2PePacket) throw IllegalArgumentException()
            stream.write(unconnectedBlabber)
            stream.writeAddress(obj.remoteBindingAddress)
            stream.writeShort(obj.mtuSize)
            stream.writeLong(obj.clientUuid)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            stream.skipBytes(unconnectedBlabber.size)
            return OpenConnectionRequest2PePacket(
                    remoteBindingAddress = stream.readAddress(),
                    mtuSize = stream.readShort().toInt(),
                    clientUuid = stream.readLong()
            )
        }
    }
}