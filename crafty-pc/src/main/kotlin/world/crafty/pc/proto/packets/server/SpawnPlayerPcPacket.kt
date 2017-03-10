package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.metadata.PlayerMetadata
import world.crafty.pc.proto.PcPacket
import java.util.*

class SpawnPlayerPcPacket(
        val entityId : Int,
        val playerUuid : UUID,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Byte,
        val pitch: Byte,
        val metadata: PlayerMetadata
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x05
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SpawnPlayerPcPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.entityId)
            stream.writeUuid(obj.playerUuid)
            stream.writeDouble(obj.x)
            stream.writeDouble(obj.y)
            stream.writeDouble(obj.z)
            stream.writeByte(obj.yaw)
            stream.writeByte(obj.pitch)
            obj.metadata.writeToStream(stream)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            throw NotImplementedError() // TODO
            /*
            return SpawnPlayerPcPacket(
                    entityId = stream.readUnsignedVarInt(),
                    playerUuid = stream.readUuid(),
                    x = stream.readDouble(),
                    y = stream.readDouble(),
                    z = stream.readDouble(),
                    yaw = stream.readByte(),
                    pitch = stream.readByte(),
                    metadata = stream.readBytes(stream.available())
            )
            */
        }
    }
}