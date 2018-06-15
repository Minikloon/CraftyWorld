package world.crafty.pc.proto.packets.server

import org.joml.Vector3f
import world.crafty.common.Angle256
import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.metadata.PcMetadataMap
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.readPcLocation
import world.crafty.pc.writePcLocation
import java.util.*

class SpawnMobPcPacket(
        val entityId: Int,
        val entityUuid: UUID,
        val typeId: Int,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Angle256,
        val pitch: Angle256,
        val headPitch: Angle256,
        val velX: Int,
        val velY: Int,
        val velZ: Int,
        val metadata: PcMetadataMap
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec

    companion object Codec : PcPacketCodec() {
        override val id = 0x03
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SpawnMobPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeUuid(obj.entityUuid)
            stream.writeSignedVarInt(obj.typeId)
            stream.writeDouble(obj.x)
            stream.writeDouble(obj.y)
            stream.writeDouble(obj.z)
            stream.writeAngle(obj.yaw)
            stream.writeAngle(obj.pitch)
            stream.writeAngle(obj.headPitch)
            stream.writeShort(obj.velX)
            stream.writeShort(obj.velY)
            stream.writeShort(obj.velZ)
            PcMetadataMap.Codec.serialize(obj.metadata, stream)
        }

        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SpawnMobPcPacket(
                    entityId = stream.readSignedVarInt(),
                    entityUuid = stream.readUuid(),
                    typeId = stream.readSignedVarInt(),
                    x = stream.readDouble(),
                    y = stream.readDouble(),
                    z = stream.readDouble(),
                    yaw = stream.readAngle(),
                    pitch = stream.readAngle(),
                    headPitch = stream.readAngle(),
                    velX = stream.readShort().toInt(),
                    velY = stream.readShort().toInt(),
                    velZ = stream.readShort().toInt(),
                    metadata = PcMetadataMap.Codec.deserialize(stream)
            )
        }
    }
}