package world.crafty.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class DisconnectPlayerCraftyPacket(
        val message: String
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is DisconnectPlayerCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedString(obj.message)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return DisconnectPlayerCraftyPacket(
                    message = stream.readUnsignedString()
            )
        }
    }
}