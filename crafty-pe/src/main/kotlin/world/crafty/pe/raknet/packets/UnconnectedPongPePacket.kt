package world.crafty.pe.raknet.packets

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.raknet.unconnectedBlabber

class UnconnectedPongServerPePacket(
        val pingId: Long,
        val serverId: Long,
        val extra: ByteArray
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec

    object Codec : PePacketCodec() {
        override val id = 0x1c
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is UnconnectedPongServerPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.pingId)
            stream.writeLong(obj.serverId)
            stream.write(unconnectedBlabber)
            stream.write(obj.extra)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val pingId = stream.readLong()
            val serverId = stream.readLong()
            stream.skipBytes(unconnectedBlabber.size)
            return UnconnectedPongServerPePacket(
                    pingId = pingId,
                    serverId = serverId,
                    extra = stream.readRemainingBytes()
            )
        }
    }
}