package world.crafty.pe.proto.packets.mixed

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

enum class ChatType(val usesSource: Boolean) { 
    RAW(false), 
    CHAT(true), 
    TRANSLATION(false), 
    POPUP(false), 
    TIP(false) 
}

class ChatPePacket(
        val type: ChatType,
        val source: String?,
        val text: String
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x0a
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ChatPePacket) throw IllegalArgumentException()
            stream.writeByte(obj.type.ordinal)
            if(obj.type.usesSource)
                stream.writeString(obj.source ?: throw IllegalStateException("ChatPePacket of type ${obj.type} requires a non-null source"))
            stream.writeString(obj.text)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val type = ChatType.values()[stream.readByte().toInt()]
            return ChatPePacket(
                    type = type,
                    source = if(type.usesSource) stream.readString() else null,
                    text = stream.readString()
            )
        }
    }
}