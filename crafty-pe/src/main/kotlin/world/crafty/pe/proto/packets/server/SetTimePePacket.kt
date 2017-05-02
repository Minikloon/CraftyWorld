package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class SetTimePePacket(
        val time: Int,
        val started: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x0b
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetTimePePacket) throw IllegalArgumentException()
            stream.writeZigzagVarInt(obj.time)
            stream.writeBoolean(obj.started)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetTimePePacket(
                    time = stream.readZigzagVarInt(),
                    started = stream.readBoolean()
            )
        }
    }
}