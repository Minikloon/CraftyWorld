package world.crafty.proto.packets.client

import org.joml.Vector3fc
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class SetPlayerPosCraftyPacket(
        val coords: Vector3fc,
        val onGround: Boolean
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetPlayerPosCraftyPacket) throw IllegalArgumentException()
            stream.writeVector3f(obj.coords)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return SetPlayerPosCraftyPacket(
                    coords = stream.readVector3f(),
                    onGround = stream.readBoolean()
            )
        }
    }
}