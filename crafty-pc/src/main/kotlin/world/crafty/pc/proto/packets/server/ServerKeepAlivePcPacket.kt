package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class ServerKeepAlivePcPacket(
        val confirmId: Int
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x1F
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ServerKeepAlivePcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.confirmId)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ServerKeepAlivePcPacket(
                    confirmId = stream.readSignedVarInt()
            )
        }
    }
}