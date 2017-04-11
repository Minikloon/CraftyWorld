package world.crafty.proto.packets.client

import world.crafty.common.Angle256
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class SetPlayerLookCraftyPacket(
        val headPitch: Angle256,
        val headYaw: Angle256,
        val bodyYaw: Angle256
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetPlayerLookCraftyPacket) throw IllegalArgumentException()
            stream.writeAngle(obj.headPitch)
            stream.writeAngle(obj.headYaw)
            stream.writeAngle(obj.bodyYaw)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return SetPlayerLookCraftyPacket(
                    headPitch = stream.readAngle(),
                    headYaw = stream.readAngle(),
                    bodyYaw = stream.readAngle()
            )
        }
    }
}