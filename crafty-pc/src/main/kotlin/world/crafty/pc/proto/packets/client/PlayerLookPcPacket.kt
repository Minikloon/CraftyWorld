package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class PlayerLookPcPacket(
        val yaw: Float,
        val pitch: Float,
        val onGround: Boolean
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x0E
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerLookPcPacket) throw IllegalArgumentException()
            stream.writeFloat(obj.yaw)
            stream.writeFloat(obj.pitch)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return PlayerLookPcPacket(
                    yaw = stream.readFloat(),
                    pitch = stream.readFloat(),
                    onGround = stream.readBoolean()
            )
        }
    }
}