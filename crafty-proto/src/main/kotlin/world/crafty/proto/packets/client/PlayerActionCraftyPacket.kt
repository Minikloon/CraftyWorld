package world.crafty.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class PlayerActionCraftyPacket(
        val action: PlayerAction
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerActionCraftyPacket) throw IllegalArgumentException()
            stream.writeByte(obj.action.ordinal)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return PlayerActionCraftyPacket(
                    action = PlayerAction.values()[stream.readUnsignedByte()]
            )
        }
    }
}

enum class PlayerAction {
    START_SNEAK,
    STOP_SNEAK,
    START_SPRINT,
    STOP_SPRINT,
    DROP_ITEM,
    LEAVE_BED
}