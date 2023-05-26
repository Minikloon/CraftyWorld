package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class ServerKeepAlivePcPacket(
        val confirmId: Long
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x20
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ServerKeepAlivePcPacket) throw IllegalArgumentException()
            stream.writeLong(obj.confirmId)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ServerKeepAlivePcPacket(
                    confirmId = stream.readLong()
            )
        }
    }
}