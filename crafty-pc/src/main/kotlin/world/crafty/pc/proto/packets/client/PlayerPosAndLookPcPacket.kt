package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class PlayerPosAndLookPcPacket(
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
        val pitch: Float,
        val onGround: Boolean
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x0D
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerPosAndLookPcPacket) throw IllegalArgumentException()
            stream.writeDouble(obj.x)
            stream.writeDouble(obj.y)
            stream.writeDouble(obj.z)
            stream.writeFloat(obj.yaw)
            stream.writeFloat(obj.pitch)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return PlayerPosAndLookPcPacket(
                    x = stream.readDouble(),
                    y = stream.readDouble(),
                    z = stream.readDouble(),
                    yaw = stream.readFloat(),
                    pitch = stream.readFloat(),
                    onGround = stream.readBoolean()
            )
        }
    }
}