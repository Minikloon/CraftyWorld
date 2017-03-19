package world.crafty.proto.mixed

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class ChatMessageCraftyPacket(
        val text: String
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ChatMessageCraftyPacket) throw IllegalArgumentException()
            stream.writeString(obj.text)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return ChatMessageCraftyPacket(
                    text = stream.readString()
            )
        }
    }
}