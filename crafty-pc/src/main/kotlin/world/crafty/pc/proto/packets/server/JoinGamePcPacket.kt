package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class JoinGamePcPacket(
        val eid: Int,
        val gamemode: Int,
        val dimension: Int,
        val difficulty: Int,
        val maxPlayers: Int,
        val levelType: String,
        val reducedDebug: Boolean
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x23
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is JoinGamePcPacket) throw IllegalArgumentException()
            stream.writeInt(obj.eid)
            stream.writeByte(obj.gamemode)
            stream.writeInt(obj.dimension)
            stream.writeByte(obj.difficulty)
            stream.writeByte(obj.maxPlayers)
            stream.writeString(obj.levelType)
            stream.writeBoolean(obj.reducedDebug)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return JoinGamePcPacket(
                    eid = stream.readInt(),
                    gamemode = stream.readUnsignedByte(),
                    dimension = stream.readInt(),
                    difficulty = stream.readUnsignedByte(),
                    maxPlayers = stream.readUnsignedByte(),
                    levelType = stream.readString(),
                    reducedDebug = stream.readBoolean()
            )
        }
    }
}