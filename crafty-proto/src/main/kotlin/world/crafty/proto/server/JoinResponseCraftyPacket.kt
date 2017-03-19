package world.crafty.proto.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class JoinResponseCraftyPacket(
        val accepted: Boolean,
        val playerId: Int
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is JoinResponseCraftyPacket) throw IllegalArgumentException()
            stream.writeBoolean(obj.accepted)
            stream.writeUnsignedVarInt(obj.playerId)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return JoinResponseCraftyPacket(
                    accepted = stream.readBoolean(),
                    playerId = stream.readUnsignedVarInt()
            )
        }
    }
}