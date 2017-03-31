package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class SetCommandsEnabledPePacket(
        val enabled: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x3c
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetCommandsEnabledPePacket) throw IllegalArgumentException()
            stream.writeBoolean(obj.enabled)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetCommandsEnabledPePacket(
                    enabled = stream.readBoolean()
            )
        }
    }
}