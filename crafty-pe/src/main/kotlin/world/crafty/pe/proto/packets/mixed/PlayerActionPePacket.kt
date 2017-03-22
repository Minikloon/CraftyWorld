package world.crafty.pe.proto.packets.mixed

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

enum class PlayerAction(val id: Int) {
    START_BREAK(0),
    ABORT_BREAK(1),
    STOP_BREAK(2),
    UNKNOWN_3(3),
    UNKNOWN_4(4),
    RELEASE_ITEM(5),
    STOP_SLEEPING(6),
    RESPAWN(7),
    JUMP(8),
    START_SPRINT(9),
    STOP_SPRINT(10),
    START_SNEAK(11),
    STOP_SNEAK(12),
    DIMENSION_CHANGE(13),
    ABORT_DIMENSION_CHANGE(14),
    START_GLIDE(15),
    STOP_GLIDE(16)
}
class PlayerActionPePacket(
        val entityId: Long,
        val action: PlayerAction,
        val x: Int,
        val y: Int,
        val z: Int,
        val blockFace: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x24
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
                    action = PlayerAction.values()[stream.readZigzagVarInt()],
                    x = stream.readZigzagVarInt(),
                    y = stream.readUnsignedVarInt(),
                    z = stream.readZigzagVarInt(),
                    blockFace = stream.readZigzagVarInt()
            )
        }
    }
}