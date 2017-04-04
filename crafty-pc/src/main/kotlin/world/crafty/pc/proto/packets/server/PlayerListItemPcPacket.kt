package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.mojang.ProfileProperty
import world.crafty.pc.proto.McChat
import world.crafty.pc.proto.PcPacket
import java.util.*

enum class PlayerListAction { ADD, SET_GAMEMODE, SET_LATENCY, SET_DISPLAY_NAME, REMOVE }

class PlayerListItemPcPacket(
        val action: PlayerListAction,
        val items: List<PlayerListPcItem>
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec

    companion object Codec : PcPacketCodec() {
        override val id = 0x2D
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if (obj !is PlayerListItemPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.action.ordinal)
            stream.writeSignedVarInt(obj.items.size)
            obj.items.forEach { it.codec.serialize(it, stream) }
        }

        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            val action = PlayerListAction.values()[stream.readSignedVarInt()]
            val actionCodec = actionCodecs[action] ?: throw IllegalStateException("Unknown player list action id $action")
            val items = (0 until stream.readSignedVarInt()).map {
                actionCodec.deserialize(stream)
            }
            return PlayerListItemPcPacket(action, items)
        }

        val actionCodecs = listOf(
                PlayerListPcAdd.Codec,
                PlayerListPcSetGamemode.Codec,
                PlayerListPcSetLatency.Codec,
                PlayerListPcSetDisplayName.Codec,
                PlayerListPcRemove.Codec
        ).associateBy { it.action }
    }
}

abstract class PlayerListPcItem(val uuid: UUID) {
    abstract val action: PlayerListAction
    abstract val codec: McCodec<PlayerListPcItem>
}

abstract class PlayerListItemCodec : McCodec<PlayerListPcItem> {
    abstract val action: PlayerListAction
}

class PlayerListPcAdd(
        uuid: UUID,
        val name: String,
        val properties: List<ProfileProperty>,
        val gamemode: Int,
        val ping: Int,
        val displayName: McChat? = null
) : PlayerListPcItem(uuid) {
    override val action = Codec.action
    override val codec = Codec
    object Codec : PlayerListItemCodec() {
        override val action = PlayerListAction.ADD
        override fun serialize(obj: PlayerListPcItem, stream: MinecraftOutputStream) {
            if(obj !is PlayerListPcAdd) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
            stream.writeSignedString(obj.name)
            stream.writeSignedVarInt(obj.properties.size)
            obj.properties.forEach {
                stream.writeSignedString(it.name)
                stream.writeSignedString(it.value)
                stream.writeBoolean(it.signature != null)
                val signature = it.signature
                if (signature != null)
                    stream.writeSignedString(signature)
            }
            stream.writeSignedVarInt(obj.gamemode)
            stream.writeSignedVarInt(obj.ping)
            stream.writeBoolean(obj.displayName != null)
            if (obj.displayName != null)
                stream.writeJson(obj.displayName)
        }
        override fun deserialize(stream: MinecraftInputStream): PlayerListPcItem {
            return PlayerListPcAdd(
                    uuid = stream.readUuid(),
                    name = stream.readSignedString(),
                    properties = (0 until stream.readSignedVarInt()).map {
                        ProfileProperty(
                                name = stream.readSignedString(),
                                value = stream.readSignedString(),
                                signature = if (stream.readBoolean()) stream.readSignedString() else null
                        )
                    },
                    gamemode = stream.readSignedVarInt(),
                    ping = stream.readSignedVarInt(),
                    displayName = if (stream.readBoolean()) stream.readJson(McChat::class.java) else null
            )
        }
    }
}
class PlayerListPcSetGamemode(
        uuid: UUID,
        val gamemode: Int
) : PlayerListPcItem(uuid) {
    override val action = Codec.action
    override val codec = Codec
    object Codec : PlayerListItemCodec() {
        override val action = PlayerListAction.SET_GAMEMODE
        override fun serialize(obj: PlayerListPcItem, stream: MinecraftOutputStream) {
            if(obj !is PlayerListPcSetGamemode) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
            stream.writeSignedVarInt(obj.gamemode)
        }
        override fun deserialize(stream: MinecraftInputStream): PlayerListPcSetGamemode {
            return PlayerListPcSetGamemode(
                    uuid = stream.readUuid(),
                    gamemode = stream.readSignedVarInt()
            )
        }
    }
}
class PlayerListPcSetLatency(
        uuid: UUID,
        val ping: Int
) : PlayerListPcItem(uuid) {
    override val action = Codec.action
    override val codec = Codec
    object Codec : PlayerListItemCodec() {
        override val action = PlayerListAction.SET_LATENCY
        override fun serialize(obj: PlayerListPcItem, stream: MinecraftOutputStream) {
            if(obj !is PlayerListPcSetLatency) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
            stream.writeSignedVarInt(obj.ping)
        }
        override fun deserialize(stream: MinecraftInputStream): PlayerListPcSetLatency {
            return PlayerListPcSetLatency(
                    uuid = stream.readUuid(),
                    ping = stream.readSignedVarInt()
            )
        }
    }
}
class PlayerListPcSetDisplayName(
        uuid: UUID,
        val displayName: String?
) : PlayerListPcItem(uuid) {
    override val action = Codec.action
    override val codec = Codec
    object Codec : PlayerListItemCodec() {
        override val action = PlayerListAction.SET_DISPLAY_NAME
        override fun serialize(obj: PlayerListPcItem, stream: MinecraftOutputStream) {
            if(obj !is PlayerListPcSetDisplayName) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
            if(obj.displayName != null)
                stream.writeSignedString(obj.displayName)
        }
        override fun deserialize(stream: MinecraftInputStream): PlayerListPcSetDisplayName {
            return PlayerListPcSetDisplayName(
                    uuid = stream.readUuid(),
                    displayName = if(stream.readBoolean()) stream.readSignedString() else null
            )
        }
    }
}
class PlayerListPcRemove(
        uuid: UUID
) : PlayerListPcItem(uuid) {
    override val action = Codec.action
    override val codec = Codec
    object Codec : PlayerListItemCodec() {
        override val action = PlayerListAction.REMOVE
        override fun serialize(obj: PlayerListPcItem, stream: MinecraftOutputStream) {
            if(obj !is PlayerListPcRemove) throw IllegalArgumentException()
            stream.writeUuid(obj.uuid)
        }
        override fun deserialize(stream: MinecraftInputStream): PlayerListPcRemove {
            return PlayerListPcRemove(
                    uuid = stream.readUuid()
            )
        }
    }
}