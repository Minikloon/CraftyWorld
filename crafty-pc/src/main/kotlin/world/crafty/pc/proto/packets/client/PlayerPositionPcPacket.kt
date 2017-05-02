package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class PlayerPositionPcPacket(
        val x: Double,
        val y: Double,
        val z: Double,
        val onGround: Boolean
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x0C
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerPositionPcPacket) throw IllegalArgumentException()
            stream.writeDouble(obj.x)
            stream.writeDouble(obj.y)
            stream.writeDouble(obj.z)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return PlayerPositionPcPacket(
                    x = stream.readDouble(),
                    y = stream.readDouble(),
                    z = stream.readDouble(),
                    onGround = stream.readBoolean()
            )
        }
    }
}