package world.crafty.pe.proto.packets.server

import org.joml.Vector2f
import org.joml.Vector3f
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

enum class GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }
class StartGamePePacket(
        val entityId: Long,
        val runtimeEntityId: Long,
        val spawn: Vector3f,
        val yawAndPitch: Vector2f,
        val seed: Int,
        val dimension: Int,
        val generator: Int,
        val gamemode: GameMode,
        val difficulty: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val achievementsDisabled: Boolean,
        val dayCycleStopTime: Int,
        val eduEdition: Boolean,
        val rainLevel: Float,
        val lightningLevel: Float,
        val enableCommands: Boolean,
        val resourcePackRequired: Boolean,
        val levelId: String,
        val worldName: String
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x0c
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is StartGamePePacket) throw IllegalArgumentException()
            stream.writeSignedVarLong(obj.entityId)
            stream.writeUnsignedVarLong(obj.runtimeEntityId)
            stream.writeVector3fLe(obj.spawn)
            stream.writeVector2fLe(obj.yawAndPitch)
            stream.writeSignedVarInt(obj.seed)
            stream.writeSignedVarInt(obj.dimension)
            stream.writeSignedVarInt(obj.generator)
            stream.writeSignedVarInt(obj.gamemode.ordinal)
            stream.writeSignedVarInt(obj.difficulty)
            stream.writeSignedVarInt(obj.x)
            stream.writeSignedVarInt(obj.y)
            stream.writeSignedVarInt(obj.z)
            stream.writeBoolean(obj.achievementsDisabled)
            stream.writeSignedVarInt(obj.dayCycleStopTime)
            stream.writeBoolean(obj.eduEdition)
            stream.writeFloatLe(obj.rainLevel)
            stream.writeFloatLe(obj.lightningLevel)
            stream.writeBoolean(obj.enableCommands)
            stream.writeBoolean(obj.resourcePackRequired)
            stream.writeString(obj.levelId)
            stream.writeString(obj.worldName)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return StartGamePePacket(
                    entityId = stream.readSignedVarLong(),
                    runtimeEntityId = stream.readUnsignedVarLong(),
                    spawn = stream.readVector3fLe(),
                    yawAndPitch = stream.readVector2fLe(),
                    seed = stream.readSignedVarInt(),
                    dimension = stream.readSignedVarInt(),
                    generator = stream.readSignedVarInt(),
                    gamemode = GameMode.values()[stream.readSignedVarInt()],
                    difficulty = stream.readSignedVarInt(),
                    x = stream.readSignedVarInt(),
                    y = stream.readSignedVarInt(),
                    z = stream.readSignedVarInt(),
                    achievementsDisabled = stream.readBoolean(),
                    dayCycleStopTime = stream.readSignedVarInt(),
                    eduEdition = stream.readBoolean(),
                    rainLevel = stream.readFloat(),
                    lightningLevel = stream.readFloat(),
                    enableCommands = stream.readBoolean(),
                    resourcePackRequired = stream.readBoolean(),
                    levelId = stream.readString(),
                    worldName = stream.readString()
            )
        }
    }
}