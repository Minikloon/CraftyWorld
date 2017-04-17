package world.crafty.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class PingCraftyPacket(
        val id: Int
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PingCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.id)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return PingCraftyPacket(
                    id = stream.readUnsignedVarInt()
            )
        }
    }
}