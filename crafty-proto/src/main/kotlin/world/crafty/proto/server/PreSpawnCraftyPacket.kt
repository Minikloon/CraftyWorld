package world.crafty.proto.server

import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket
import world.crafty.proto.GameMode

class PreSpawnCraftyPacket(
        val entityId: Long,
        val spawnLocation: Location,
        val dimension: Int,
        val gamemode: GameMode
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PreSpawnCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeLocation(obj.spawnLocation)
            stream.writeByte(obj.dimension)
            stream.writeByte(obj.gamemode.ordinal)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return PreSpawnCraftyPacket(
                    entityId = stream.readUnsignedVarLong(),
                    spawnLocation = stream.readLocation(),
                    dimension = stream.readUnsignedByte(),
                    gamemode = GameMode.values()[stream.readUnsignedByte()]
            )
        }
    }
}