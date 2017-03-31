package world.crafty.proto.server

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket
import world.crafty.proto.CraftySkin
import java.util.*

class UpdatePlayerListCraftyPacket(
        val items: List<CraftyPlayerListItem>
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is UpdatePlayerListCraftyPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.items.size)
            obj.items.forEach {
                stream.writeByte(it.type.ordinal)
                it.codec.serialize(it, stream)
            }
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {            
            return UpdatePlayerListCraftyPacket(
                    items = (1..stream.readUnsignedVarInt()).map {
                        val type = PlayerListItemType.values()[stream.readUnsignedByte()]
                        val codec = typeToCodec[type] ?: throw IllegalStateException("Unknown player list codec for type $type")
                        codec.deserialize(stream)
                    }
            )
        }
        private val typeToCodec = listOf(
                PlayerListAdd.Codec,
                PlayerListRemove.Codec
        ).associateBy { it.type }
    }
}

enum class PlayerListItemType { ADD, REMOVE }
abstract class CraftyPlayerListItem(
        val uuid: UUID
) {
        abstract val type: PlayerListItemType
        abstract val codec: PlayerListItemCodec
}

abstract class PlayerListItemCodec : McCodec<CraftyPlayerListItem> {
    abstract val type: PlayerListItemType
}

class PlayerListAdd(
        uuid: UUID,
        val entityId: Long,
        val name: String,
        val ping: Int,
        val skin: CraftySkin
) : CraftyPlayerListItem(uuid) {
    override val type = Codec.type
    override val codec = Codec
    object Codec : PlayerListItemCodec() {
        override val type = PlayerListItemType.ADD
        override fun serialize(obj: CraftyPlayerListItem, stream: MinecraftOutputStream) {
            if(obj !is PlayerListAdd) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeUnsignedString(obj.name)
            stream.writeUnsignedVarInt(obj.ping)
            CraftySkin.Codec.serialize(obj.skin, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPlayerListItem {
            return PlayerListAdd(
                    uuid = stream.readUuid(),
                    entityId = stream.readUnsignedVarLong(),
                    name = stream.readUnsignedString(),
                    ping = stream.readUnsignedVarInt(),
                    skin = CraftySkin.Codec.deserialize(stream)
            )
        }
    }
}

class PlayerListRemove(
    uuid: UUID
) : CraftyPlayerListItem(uuid) {
    override val type = Codec.type
    override val codec = Codec
    object Codec : PlayerListItemCodec() {
        override val type = PlayerListItemType.REMOVE
        override fun serialize(obj: CraftyPlayerListItem, stream: MinecraftOutputStream) {
            if(obj !is PlayerListRemove) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPlayerListItem {
            return PlayerListRemove(
                    uuid = stream.readUuid()
            )
        }
    }
}