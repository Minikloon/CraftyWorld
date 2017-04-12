package world.crafty.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class QuitCraftyPacket(
        val craftyPlayerId: Int
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is QuitCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.craftyPlayerId)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return QuitCraftyPacket(
                    craftyPlayerId = stream.readUnsignedVarInt()
            )
        }
    }
}