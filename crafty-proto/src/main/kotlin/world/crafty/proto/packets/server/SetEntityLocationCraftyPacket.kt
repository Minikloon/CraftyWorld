package world.crafty.proto.packets.server

import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class SetEntityLocationCraftyPacket(
        val entityId: Long,
        val location: Location,
        val onGround: Boolean
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetEntityLocationCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeLocation(obj.location)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return SetEntityLocationCraftyPacket(
                    entityId = stream.readUnsignedVarLong(),
                    location = stream.readLocation(),
                    onGround = stream.readBoolean()
            )
        }
    }
}