package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class DisconnectPePacket(
        val message: String?
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x05
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is DisconnectPePacket) throw IllegalArgumentException()
            val skipMessage = obj.message == null
            stream.writeBoolean(skipMessage)
            if(!skipMessage)
                stream.writeUnsignedString(obj.message!!)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return DisconnectPePacket(
                    message = if(stream.readBoolean()) null else stream.readUnsignedString()
            )
        }
    }
}