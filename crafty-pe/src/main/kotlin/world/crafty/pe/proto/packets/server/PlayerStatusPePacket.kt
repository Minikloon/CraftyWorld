package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

enum class PlayerStatus { LOGIN_ACCEPTED, LOGIN_FAILED_CLIENT, LOGIN_FAILED_SERVER, SPAWN }
class PlayerStatusPePacket(
        val status: PlayerStatus
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x02
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerStatusPePacket) throw IllegalStateException()
            stream.writeInt(obj.status.ordinal)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return PlayerStatusPePacket(
                    status = PlayerStatus.values()[stream.readInt()]
            )
        }
    }
}