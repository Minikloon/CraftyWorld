package world.crafty.pe.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

enum class PePlayerAction {
    START_BREAK,
    ABORT_BREAK,
    STOP_BREAK,
    UNKNOWN_3,
    UNKNOWN_4,
    RELEASE_ITEM,
    STOP_SLEEPING,
    RESPAWN,
    JUMP,
    START_SPRINT,
    STOP_SPRINT,
    START_SNEAK,
    STOP_SNEAK,
    START_DIMENSION_CHANGE,
    ABORT_DIMENSION_CHANGE,
    START_GLIDE,
    STOP_GLIDE
}
class PlayerActionPePacket(
        val entityId: Long,
        val action: PePlayerAction,
        val x: Int,
        val y: Int,
        val z: Int,
        val blockFace: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x25
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerActionPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeZigzagVarInt(obj.action.ordinal)
            stream.writeZigzagVarInt(obj.x)
            stream.writeUnsignedVarInt(obj.y)
            stream.writeZigzagVarInt(obj.z)
            stream.writeZigzagVarInt(obj.blockFace)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return PlayerActionPePacket(
                    entityId = stream.readUnsignedVarLong(),
                    action = PePlayerAction.values()[stream.readZigzagVarInt()],
                    x = stream.readZigzagVarInt(),
                    y = stream.readUnsignedVarInt(),
                    z = stream.readZigzagVarInt(),
                    blockFace = stream.readZigzagVarInt()
            )
        }
    }
}