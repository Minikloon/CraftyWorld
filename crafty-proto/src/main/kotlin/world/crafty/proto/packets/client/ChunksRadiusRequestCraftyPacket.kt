package world.crafty.proto.packets.client

import org.joml.Vector3fc
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class ChunksRadiusRequestCraftyPacket(
        val pos: Vector3fc,
        val ignoreRadius: Int,
        val radius: Int
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ChunksRadiusRequestCraftyPacket) throw IllegalArgumentException()
            stream.writeVector3fLe(obj.pos)
            stream.writeUnsignedVarInt(obj.ignoreRadius)
            stream.writeUnsignedVarInt(obj.radius)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return ChunksRadiusRequestCraftyPacket(
                    pos = stream.readVector3fLe(),
                    ignoreRadius = stream.readUnsignedVarInt(),
                    radius = stream.readUnsignedVarInt()
            )
        }
    }
}