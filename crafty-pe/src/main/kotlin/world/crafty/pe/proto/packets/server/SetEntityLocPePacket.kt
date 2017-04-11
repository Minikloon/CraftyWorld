package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.PeLocation
import world.crafty.pe.proto.PePacket
import world.crafty.pe.readPeLocation
import world.crafty.pe.writePeLocation

class SetEntityLocPePacket(
        val entityId: Long,
        val location: PeLocation
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x13
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetEntityLocPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writePeLocation(obj.location)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetEntityLocPePacket(
                    entityId = stream.readUnsignedVarLong(),
                    location = stream.readPeLocation()
            )
        }
    }
}