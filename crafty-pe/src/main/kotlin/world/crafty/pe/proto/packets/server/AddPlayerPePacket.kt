package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PeItem
import world.crafty.pe.proto.PePacket
import java.util.*

class AddPlayerPePacket(
        val uuid: UUID,
        val username: String,
        val entityId: Long,
        val runtimeEntityId: Long,
        val x: Float,
        val y: Float,
        val z: Float,
        val speedX: Float,
        val speedY: Float,
        val speedZ: Float,
        val headPitch: Float,
        val headYaw: Float,
        val bodyYaw: Float,
        val item: PeItem,
        val metadata: Any
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x0d
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AddPlayerPePacket) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
            stream.writeUnsignedString(obj.username)
            stream.writeSignedVarLong(obj.entityId)
            stream.writeUnsignedVarLong(obj.runtimeEntityId)
            stream.writeFloatLe(obj.x)
            stream.writeFloatLe(obj.y)
            stream.writeFloatLe(obj.z)
            stream.writeFloatLe(obj.speedX)
            stream.writeFloatLe(obj.speedY)
            stream.writeFloatLe(obj.speedZ)
            stream.writeFloatLe(obj.headPitch)
            stream.writeFloatLe(obj.headYaw)
            stream.writeFloatLe(obj.bodyYaw)
            PeItem.Codec.serialize(obj.item, stream)
            
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            
        }
    }
}