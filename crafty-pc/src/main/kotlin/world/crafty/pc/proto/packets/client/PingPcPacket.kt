package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class PingPcPacket(
        val epoch: Long
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x01
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PingPcPacket) throw IllegalArgumentException()
            stream.writeLong(obj.epoch)
        }
        override fun deserialize(stream: MinecraftInputStream) : PcPacket {
            return PingPcPacket(
                    epoch = stream.readLong()
            )
        }
    }
}