package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class SwingArmPcPacket(
        val hand: Int
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x1D
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SwingArmPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.hand)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SwingArmPcPacket(
                    hand = stream.readSignedVarInt()
            )
        }
    }
}