package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.metadata.PeMetadataMap
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
        val itemInHand: PeItem,
        val metadata: PeMetadataMap
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x0d
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AddPlayerPePacket) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
            stream.writeUnsignedString(obj.username)
            stream.writeZigzagVarLong(obj.entityId)
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
            PeItem.Codec.serialize(obj.itemInHand, stream)
            PeMetadataMap.Codec.serialize(obj.metadata, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return AddPlayerPePacket(
                    uuid = stream.readUuid(),
                    username = stream.readUnsignedString(),
                    entityId = stream.readSignedVarLong(),
                    runtimeEntityId = stream.readUnsignedVarLong(),
                    x = stream.readFloatLe(),
                    y = stream.readFloatLe(),
                    z = stream.readFloatLe(),
                    speedX = stream.readFloatLe(),
                    speedY = stream.readFloatLe(),
                    speedZ = stream.readFloatLe(),
                    headPitch = stream.readFloatLe(),
                    headYaw = stream.readFloatLe(),
                    bodyYaw = stream.readFloatLe(),
                    itemInHand = PeItem.Codec.deserialize(stream),
                    metadata = PeMetadataMap.Codec.deserialize(stream)
            )
        }
    }
}