package world.crafty.pe.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class ConnectedPingPePacket(
        val pingTimestamp: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x00
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ConnectedPingPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.pingTimestamp)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ConnectedPingPePacket(
                    pingTimestamp = stream.readLong()
            )
        }
    }
}