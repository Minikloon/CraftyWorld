package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class ConnectedPongPePacket(
        val pingTimestamp: Long,
        val pongTimestamp: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x03
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ConnectedPongPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.pingTimestamp)
            stream.writeLong(obj.pongTimestamp)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ConnectedPongPePacket(
                    pingTimestamp = stream.readLong(),
                    pongTimestamp = stream.readLong()
            )
        }
    }
}