package world.crafty.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class JoinResponseCraftyPacket private constructor(
        val accepted: Boolean,
        val playerId: Int?,
        val prespawn: PreSpawnCraftyPacket?
) : CraftyPacket() {
    constructor(playerId: Int, prespawn: PreSpawnCraftyPacket) : this(true, playerId, prespawn)
    
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is JoinResponseCraftyPacket) throw IllegalArgumentException()
            stream.writeBoolean(obj.accepted)
            if(obj.accepted) {
                stream.writeUnsignedVarInt(obj.playerId!!)
                obj.prespawn!!.serialize(stream)
            }
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            val accepted = stream.readBoolean()
            return JoinResponseCraftyPacket(
                    accepted = accepted,
                    playerId = if(accepted) stream.readUnsignedVarInt() else null,
                    prespawn = if(accepted) PreSpawnCraftyPacket.Codec.deserialize(stream) as PreSpawnCraftyPacket else null
            )
        }
    }
    
    companion object {
        val denied = JoinResponseCraftyPacket(false, null, null)
    }
}