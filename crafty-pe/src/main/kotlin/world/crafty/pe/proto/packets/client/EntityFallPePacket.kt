package world.crafty.pe.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class EntityFallPePacket(
        val distance: Float
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x26
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is EntityFallPePacket) throw IllegalArgumentException()
            stream.writeFloatLe(obj.distance)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return EntityFallPePacket(
                    distance = stream.readFloatLe()
            )
        }
    }
}