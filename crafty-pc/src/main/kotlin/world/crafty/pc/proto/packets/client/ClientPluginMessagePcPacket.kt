package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class ClientPluginMessagePcPacket(
        val channel: String,
        val data: ByteArray
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x09
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ClientPluginMessagePcPacket) throw IllegalArgumentException()
            stream.writeString(obj.channel)
            stream.write(obj.data)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ClientPluginMessagePcPacket(
                    channel = stream.readString(),
                    data = stream.readBytes(stream.available())
            )
        }
    }
}