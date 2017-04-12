package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.McChat
import world.crafty.pc.proto.PcPacket

class DisconnectPcPacket(
        val message: McChat
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x1a
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is DisconnectPcPacket) throw IllegalArgumentException()
            stream.writeJson(obj.message)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return DisconnectPcPacket(
                    message = stream.readJson(McChat::class)
            )
        }
    }
}