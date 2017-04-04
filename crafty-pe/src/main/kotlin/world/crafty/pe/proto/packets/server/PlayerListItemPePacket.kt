package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.PeSkin
import java.util.*

enum class PlayerListAction { ADD, REMOVE }

class PlayerListItemPePacket(
        val action: PlayerListAction,
        val items: List<PlayerListPeItem>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x40
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerListItemPePacket) throw IllegalArgumentException()
            stream.writeByte(obj.action.ordinal)
            stream.writeUnsignedVarInt(obj.items.size)
            obj.items.forEach {
                it.codec.serialize(it, stream)
            }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val action = PlayerListAction.values()[stream.readSignedVarInt()]
            val actionCodec = actionCodecs[action] ?: throw IllegalStateException("Unknown player list action id $action")
            val items = (0 until stream.readUnsignedVarInt()).map { 
                actionCodec.deserialize(stream)
            }
            return PlayerListItemPePacket(action, items)
        }
        
        val actionCodecs = listOf(
                PlayerListPeAdd.Codec,
                PlayerListPeRemove.Codec
        ).associateBy { it.action }
    }
}

abstract class PlayerListPeItem(val uuid: UUID) {
    abstract val action: PlayerListAction
    abstract val codec: McCodec<PlayerListPeItem>
}

abstract class PlayerListItemCodec : McCodec<PlayerListPeItem> {
    abstract val action: PlayerListAction
}

class PlayerListPeAdd(
        uuid: UUID,
        val entityId: Long,
        val name: String,
        val skin: PeSkin
) : PlayerListPeItem(uuid) {
    override val action = Codec.action
    override val codec = Codec
    object Codec : PlayerListItemCodec() {
        override val action = PlayerListAction.ADD
        override fun serialize(obj: PlayerListPeItem, stream: MinecraftOutputStream) {
            if(obj !is PlayerListPeAdd) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
            stream.writeZigzagVarLong(obj.entityId)
            stream.writeUnsignedString(obj.name)
            PeSkin.Codec.serialize(obj.skin, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): PlayerListPeItem {
            return PlayerListPeAdd(
                    uuid = stream.readUuid(),
                    entityId = stream.readSignedVarLong(),
                    name = stream.readUnsignedString(),
                    skin = PeSkin.Codec.deserialize(stream)
            )
        }
    }
}

class PlayerListPeRemove(
        uuid: UUID
) : PlayerListPeItem(uuid) {
    override val action = Codec.action
    override val codec = Codec
    object Codec : PlayerListItemCodec() {
        override val action = PlayerListAction.REMOVE
        override fun serialize(obj: PlayerListPeItem, stream: MinecraftOutputStream) {
            if(obj !is PlayerListPeRemove) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
        }
        override fun deserialize(stream: MinecraftInputStream): PlayerListPeItem {
            return PlayerListPeRemove(
                    uuid = stream.readUuid()
            )
        }
    }
}