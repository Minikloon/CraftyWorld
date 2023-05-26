package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class ClientSettingsPcPacket(
        val locale: String,
        val viewDistance: Int,
        val chatMode: Int,
        val chatColors: Boolean,
        val skinParts: Int,
        val mainHand: Int
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x03
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ClientSettingsPcPacket) throw IllegalArgumentException()
            stream.writeSignedString(obj.locale)
            stream.writeByte(obj.viewDistance)
            stream.writeSignedVarInt(obj.chatMode)
            stream.writeBoolean(obj.chatColors)
            stream.writeByte(obj.skinParts)
            stream.writeSignedVarInt(obj.mainHand)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ClientSettingsPcPacket(
                    locale = stream.readSignedString(),
                    viewDistance = stream.readByte().toInt(),
                    chatMode = stream.readSignedVarInt(),
                    chatColors = stream.readBoolean(),
                    skinParts = stream.readUnsignedByte(),
                    mainHand = stream.readSignedVarInt()
            )
        }
    }
}