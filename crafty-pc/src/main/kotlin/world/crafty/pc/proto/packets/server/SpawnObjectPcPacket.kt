package world.crafty.pc.proto.packets.server

import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.readPcLocation
import world.crafty.pc.writePcLocation
import java.util.*

class SpawnObjectPcPacket(
        val entityId: Int,
        val objectUuid: UUID,
        val objectType: Byte,
        val location: Location,
        val data: Int,
        val velX: Int,
        val velY: Int,
        val velZ: Int
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec

    companion object Codec : PcPacketCodec() {
        override val id = 0x00
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SpawnObjectPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeUuid(obj.objectUuid)
            stream.writeByte(obj.objectType)
            stream.writePcLocation(obj.location)
            stream.writeInt(obj.data)
            stream.writeShort(obj.velX)
            stream.writeShort(obj.velY)
            stream.writeShort(obj.velZ)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SpawnObjectPcPacket(
                    entityId = stream.readSignedVarInt(),
                    objectUuid = stream.readUuid(),
                    objectType = stream.readByte(),
                    location = stream.readPcLocation(),
                    data = stream.readInt(),
                    velX = stream.readShort().toInt(),
                    velY = stream.readShort().toInt(),
                    velZ = stream.readShort().toInt()
            )
        }
    }
}