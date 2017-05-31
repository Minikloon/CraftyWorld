package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class AnimationPcPacket(
        val entityId: Int,
        val animation: PcAnimation
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x06
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AnimationPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeByte(obj.animation.ordinal)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return AnimationPcPacket(
                    entityId = stream.readSignedVarInt(),
                    animation = PcAnimation.values()[stream.readUnsignedByte()]
            )
        }
    }
}

enum class PcAnimation {
    SWING_MAIN_ARM,
    TAKE_DAMAGE,
    LEAVE_BED,
    SWING_OFFHAND,
    CRITICAL_EFFECT,
    MAGIC_CRITICAL_EFFECT,
    ;
}