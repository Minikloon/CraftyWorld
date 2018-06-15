package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class SetPlayerAbilitiesPcPacket(
        val flags: Int,
        val flyingSpeed: Float,
        val fovModifier: Float
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x2D
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetPlayerAbilitiesPcPacket) throw IllegalArgumentException()
            stream.writeByte(obj.flags)
            stream.writeFloat(obj.flyingSpeed)
            stream.writeFloat(obj.fovModifier)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SetPlayerAbilitiesPcPacket(
                    flags = stream.readInt(),
                    flyingSpeed = stream.readFloat(),
                    fovModifier = stream.readFloat()
            )
        }
    }
}