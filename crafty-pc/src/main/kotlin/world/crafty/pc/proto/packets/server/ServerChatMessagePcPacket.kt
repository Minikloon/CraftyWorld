package world.crafty.pc.proto.packets.server

import io.vertx.core.json.Json
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.McChat
import world.crafty.pc.proto.PcPacket

enum class ChatPosition { CHAT_BOX, SYSTEM_CHAT_BOX, ACTION_BAR }
class ServerChatMessagePcPacket(
        val chat: McChat,
        val position: ChatPosition
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x0F
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ServerChatMessagePcPacket) throw IllegalArgumentException()
            stream.writeJson(obj.chat)
            stream.writeByte(obj.position.ordinal)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ServerChatMessagePcPacket(
                    chat = Json.decodeValue(stream.readSignedString(), McChat::class.java),
                    position = ChatPosition.values()[stream.readByte().toInt()]
            )
        }
    }
}