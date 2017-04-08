package world.crafty.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class ChatFromClientCraftyPacket(
        val text: String
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ChatFromClientCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedString(obj.text)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return ChatFromClientCraftyPacket(
                    text = stream.readUnsignedString()
            )
        }
    }
}