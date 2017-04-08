package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class EntityActionPcPacket(
        val entityId: Int,
        val action: PcEntityAction,
        val horseJumpBoost: Int
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x14
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is EntityActionPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeSignedVarInt(obj.action.ordinal)
            stream.writeSignedVarInt(obj.horseJumpBoost)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return EntityActionPcPacket(
                    entityId = stream.readSignedVarInt(),
                    action = PcEntityAction.values()[stream.readSignedVarInt()],
                    horseJumpBoost = stream.readSignedVarInt()
            )
        }
    }
}

enum class PcEntityAction {
    START_SNEAKING,
    STOP_SNEAKING,
    LEAVE_BED,
    START_SPRINTING,
    STOP_SPRINTING,
    START_HORSE_JUMP,
    STOP_HORSE_JUMP,
    OPEN_HORSE_INVENTORY,
    START_ELYTRA_FLY
}