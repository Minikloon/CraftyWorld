package world.crafty.pe.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class ConnectionRequestPePacket(
        val uuid: Long,
        val timestamp: Long,
        val secured: Boolean
) : PePacket() {
    override val codec = Codec
    override val id = Codec.id

    object Codec : PePacketCodec() {
        override val id = 0x09
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ConnectionRequestPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.uuid)
            stream.writeLong(obj.timestamp)
            stream.writeBoolean(obj.secured)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ConnectionRequestPePacket(
                    uuid = stream.readLong(),
                    timestamp = stream.readLong(),
                    secured = stream.readBoolean()
            )
        }
    }
}