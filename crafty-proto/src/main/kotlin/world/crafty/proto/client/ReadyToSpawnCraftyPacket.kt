package world.crafty.proto.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class ReadyToSpawnCraftyPacket(
        
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return ReadyToSpawnCraftyPacket()
        }
    }
}