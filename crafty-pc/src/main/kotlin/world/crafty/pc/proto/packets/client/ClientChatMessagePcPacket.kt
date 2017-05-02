package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class ClientChatMessagePcPacket(
        val text: String
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x02
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ClientChatMessagePcPacket) throw IllegalArgumentException()
            stream.writeSignedString(obj.text)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ClientChatMessagePcPacket(
                    text = stream.readSignedString()
            )
        }
    }
}