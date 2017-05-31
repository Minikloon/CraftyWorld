package world.crafty.pe.proto.packets.mixed

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class AnimationPePacket(
        val entityId: Long,
        val animation: PeAnimation
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x2D
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AnimationPePacket) throw IllegalArgumentException()
            stream.writeZigzagVarInt(obj.animation.ordinal)
            stream.writeUnsignedVarLong(obj.entityId)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return AnimationPePacket(
                    animation = PeAnimation.values()[stream.readZigzagVarInt()],
                    entityId = stream.readUnsignedVarLong()
            )
        }
    }
}

enum class PeAnimation {
    NOTHING,
    SWING_ARM,
    WAKE_UP,
    CRITICAL_HAT,
    MAGIC_CRITICAL_HIT
}