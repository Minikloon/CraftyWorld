package world.crafty.proto.server

import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket
import world.crafty.proto.CraftySkin
import java.util.*

class AddPlayerCraftyPacket(
        val uuid: UUID,
        val username: String,
        val entityId: Long,
        val location: Location,
        val skin: CraftySkin
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AddPlayerCraftyPacket) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
            stream.writeUnsignedString(obj.username)
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeLocation(obj.location)
            CraftySkin.Codec.serialize(obj.skin, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return AddPlayerCraftyPacket(
                    uuid = stream.readUuid(),
                    username = stream.readUnsignedString(),
                    entityId = stream.readUnsignedVarLong(),
                    location = stream.readLocation(),
                    skin = CraftySkin.Codec.deserialize(stream)
            )
        }
    }
}