package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class ClientKeepAlivePcPacket(
        val confirmId: Long
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x0B
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ClientKeepAlivePcPacket) throw IllegalArgumentException()
            stream.writeLong(obj.confirmId)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ClientKeepAlivePcPacket(
                    confirmId = stream.readLong()
            )
        }
    }
}