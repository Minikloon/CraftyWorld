package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.PcLocation
import world.crafty.pc.metadata.PlayerMetadata
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.writePcLocation
import java.util.*

class SpawnPlayerPcPacket(
        val entityId : Int,
        val playerUuid : UUID,
        val location: PcLocation,
        val metadata: PlayerMetadata
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x05
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SpawnPlayerPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.entityId)
            stream.writeUuid(obj.playerUuid)
            stream.writePcLocation(obj.location)
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