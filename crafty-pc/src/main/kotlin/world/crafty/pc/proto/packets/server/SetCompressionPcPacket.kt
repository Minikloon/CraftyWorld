package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class SetCompressionPcPacket(
        val threshold: Int
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x03
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetCompressionPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.threshold)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SetCompressionPcPacket(
                    threshold = stream.readSignedVarInt()
            )
        }
    }
}