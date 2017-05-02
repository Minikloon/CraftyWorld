package world.crafty.pe.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.raknet.unconnectedBlabber

class UnconnectedPingClientPePacket(
        val pingId: Long,
        val uuid: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec

    object Codec : PePacketCodec() {
        override val id = 0x01
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is UnconnectedPingClientPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.pingId)
            stream.write(unconnectedBlabber)
            stream.writeLong(obj.uuid)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val pingId = stream.readLong()
            stream.skipBytes(unconnectedBlabber.size)
            return UnconnectedPingClientPePacket(
                    pingId = pingId,
                    uuid = stream.readLong()
            )
        }
    }
}