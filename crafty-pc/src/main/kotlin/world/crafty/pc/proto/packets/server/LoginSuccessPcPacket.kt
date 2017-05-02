package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket
import java.util.*

class LoginSuccessPcPacket(
        val uuid: UUID,
        val username: String
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x02
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is LoginSuccessPcPacket) throw IllegalArgumentException()
            stream.writeSignedString(obj.uuid.toString())
            stream.writeSignedString(obj.username)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return LoginSuccessPcPacket(
                    uuid = UUID.fromString(stream.readSignedString()),
                    username = stream.readSignedString()
            )
        }
    }
}