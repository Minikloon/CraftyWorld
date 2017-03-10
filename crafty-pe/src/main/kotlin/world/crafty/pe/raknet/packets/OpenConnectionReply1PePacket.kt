package world.crafty.pe.raknet.packets

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.raknet.unconnectedBlabber

class OpenConnectionReply1PePacket(
        val serverId: Long,
        val secured: Boolean,
        val mtuSize: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x06
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is OpenConnectionReply1PePacket) throw IllegalArgumentException()
            stream.write(unconnectedBlabber)
            stream.writeLong(obj.serverId)
            stream.writeBoolean(obj.secured)
            stream.writeShort(obj.mtuSize)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            stream.skipBytes(unconnectedBlabber.size)
            return OpenConnectionReply1PePacket(
                    serverId = stream.readLong(),
                    secured = stream.readBoolean(),
                    mtuSize = stream.readShort().toInt()
            )
        }
    }
}