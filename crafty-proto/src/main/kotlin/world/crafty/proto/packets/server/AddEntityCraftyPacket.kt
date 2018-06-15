package world.crafty.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.Location
import world.crafty.proto.CraftyPacket
import world.crafty.proto.metadata.MetaValue
import java.util.*

class AddEntityCraftyPacket(
        val entityId: Int,
        val typeId: Int,
        val location: Location,
        val meta: List<MetaValue>
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AddEntityCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.entityId)
            stream.writeUnsignedVarInt(obj.typeId)
            stream.writeLocation(obj.location)
            MetaValue.serialize(obj.meta, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return AddEntityCraftyPacket(
                    entityId = stream.readUnsignedVarInt(),
                    typeId = stream.readUnsignedVarInt(),
                    location = stream.readLocation(),
                    meta = MetaValue.deserialize(stream)
            )
        }
    }
}