package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class ServerPluginMessagePcPacket(
        val channel: String,
        val data: ByteArray
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x18
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ServerPluginMessagePcPacket) throw IllegalArgumentException()
            stream.writeSignedString(obj.channel)
            stream.write(obj.data)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ServerPluginMessagePcPacket(
                    channel = stream.readSignedString(),
                    data = stream.readBytes(stream.available())
            )
        }
    }
}