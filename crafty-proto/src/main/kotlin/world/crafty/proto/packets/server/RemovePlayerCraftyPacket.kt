package world.crafty.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class RemovePlayerCraftyPacket(
        val entityId: Int
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is RemovePlayerCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.entityId)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return RemovePlayerCraftyPacket(
                    entityId = stream.readUnsignedVarInt()
            )
        }
    }
}