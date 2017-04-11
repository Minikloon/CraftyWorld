package world.crafty.pe.proto.packets.server

import world.crafty.common.Angle256
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.PeLocation
import world.crafty.pe.proto.PePacket
import world.crafty.proto.GameMode

class StartGamePePacket(
        val entityId: Long,
        val runtimeEntityId: Long,
        val spawn: PeLocation,
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
            stream.writeZigzagVarLong(obj.entityId)
            stream.writeUnsignedVarLong(obj.runtimeEntityId)
            stream.writeVector3fLe(obj.spawn.positionVec3())
            stream.writeVector2fLe(obj.spawn.anglesDegreeVec2())
            stream.writeZigzagVarInt(obj.seed)
            stream.writeZigzagVarInt(obj.dimension)
            stream.writeZigzagVarInt(obj.generator)
            stream.writeZigzagVarInt(obj.gamemode.ordinal)
            stream.writeZigzagVarInt(obj.difficulty)
            stream.writeZigzagVarInt(obj.x)
            stream.writeZigzagVarInt(obj.y)
            stream.writeZigzagVarInt(obj.z)
            stream.writeBoolean(obj.achievementsDisabled)
            stream.writeZigzagVarInt(obj.dayCycleStopTime)
            stream.writeBoolean(obj.eduEdition)
            stream.writeFloatLe(obj.rainLevel)
            stream.writeFloatLe(obj.lightningLevel)
            stream.writeBoolean(obj.enableCommands)
            stream.writeBoolean(obj.resourcePackRequired)
            stream.writeUnsignedString(obj.levelId)
            stream.writeUnsignedString(obj.worldName)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return StartGamePePacket(
                    entityId = stream.readZigzagVarLong(),
                    runtimeEntityId = stream.readUnsignedVarLong(),
                    spawn = PeLocation(stream.readVector3fLe(), bodyYaw = Angle256.fromDegrees(stream.readFloatLe()), pitch = Angle256.fromDegrees(stream.readFloatLe())),
                    seed = stream.readZigzagVarInt(),
                    dimension = stream.readZigzagVarInt(),
                    generator = stream.readZigzagVarInt(),
                    gamemode = GameMode.values()[stream.readZigzagVarInt()],
                    difficulty = stream.readZigzagVarInt(),
                    x = stream.readZigzagVarInt(),
                    y = stream.readZigzagVarInt(),
                    z = stream.readZigzagVarInt(),
                    achievementsDisabled = stream.readBoolean(),
                    dayCycleStopTime = stream.readZigzagVarInt(),
                    eduEdition = stream.readBoolean(),
                    rainLevel = stream.readFloat(),
                    lightningLevel = stream.readFloat(),
                    enableCommands = stream.readBoolean(),
                    resourcePackRequired = stream.readBoolean(),
                    levelId = stream.readUnsignedString(),
                    worldName = stream.readUnsignedString()
            )
        }
    }
}