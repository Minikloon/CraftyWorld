package world.crafty.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class PlayerAnimationCraftyPacket(
        val entityId: Int,
        val animation: Animation
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerAnimationCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.entityId)
            stream.writeUnsignedVarInt(obj.animation.ordinal)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return PlayerAnimationCraftyPacket(
                    entityId = stream.readUnsignedVarInt(),
                    animation = Animation.values()[stream.readUnsignedVarInt()]
            )
        }
    }
}

enum class Animation {
    SWING_ARM
}