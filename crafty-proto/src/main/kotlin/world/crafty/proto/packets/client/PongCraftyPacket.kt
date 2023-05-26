package world.crafty.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class PongCraftyPacket(
        val id: Long
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PongCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.id)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return PongCraftyPacket(
                    id = stream.readUnsignedVarLong()
            )
        }
    }
}