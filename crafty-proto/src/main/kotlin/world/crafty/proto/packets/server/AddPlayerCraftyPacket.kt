package world.crafty.proto.packets.server

import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket
import world.crafty.proto.CraftySkin
import world.crafty.proto.metadata.MetaValue
import java.util.*

class AddPlayerCraftyPacket(
        val craftyId: Int,
        val uuid: UUID,
        val username: String,
        val entityId: Int,
        val location: Location,
        val skin: CraftySkin,
        val meta: List<MetaValue>
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AddPlayerCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.craftyId)
            stream.writeUuid(obj.uuid)
            stream.writeUnsignedString(obj.username)
            stream.writeUnsignedVarInt(obj.entityId)
            stream.writeLocation(obj.location)
            CraftySkin.Codec.serialize(obj.skin, stream)
            MetaValue.serialize(obj.meta, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return AddPlayerCraftyPacket(
                    craftyId = stream.readUnsignedVarInt(),
                    uuid = stream.readUuid(),
                    username = stream.readUnsignedString(),
                    entityId = stream.readUnsignedVarInt(),
                    location = stream.readLocation(),
                    skin = CraftySkin.Codec.deserialize(stream),
                    meta = MetaValue.deserialize(stream)
            )
        }
    }
}