package world.crafty.pe.raknet.packets

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class DisconnectNotifyPePacket(
        
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x15
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return DisconnectNotifyPePacket()
        }
    }
}