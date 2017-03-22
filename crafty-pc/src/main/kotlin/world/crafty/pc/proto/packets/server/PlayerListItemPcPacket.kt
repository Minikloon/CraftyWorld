package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.McCodec
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.mojang.ProfileProperty
import world.crafty.pc.proto.McChat
import world.crafty.pc.proto.PcPacket
import java.util.*

class PlayerListItemPcPacket(
        val action: Int,
        val items: List<PlayerListItem>
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec

    companion object Codec : PcPacketCodec() {
        override val id = 0x2D
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if (obj !is PlayerListItemPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.action)
            stream.writeSignedVarInt(obj.items.size)
            obj.items.forEach { it.codec.serialize(it, stream) }
        }

        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            val action = stream.readSignedVarInt()
            val actionCodec = actionCodecs[action] ?: throw IllegalStateException("Unknown player list action id $action")
            val items = (0 until stream.readSignedVarInt()).map {
                actionCodec.deserialize(stream)
            }
            return PlayerListItemPcPacket(action, items)
        }

        val actionCodecs = listOf(
                PlayerListItemAdd.Codec
        ).associateBy { it.action }
    }

    abstract class PlayerListItem(val uuid: UUID) {
        abstract val action: Int
        abstract val codec: McCodec<PlayerListItem>
    }
    
    abstract class PlayerListItemCodec : McCodec<PlayerListItem> {
        abstract val action: Int
    }
    
    class PlayerListItemAdd(
            uuid: UUID,
            val name: String,
            val properties: List<ProfileProperty>,
            val gamemode: Int,
            val ping: Int,
            val displayName: McChat? = null
    ) : PlayerListItem(uuid) {
        override val action = Codec.action
        override val codec = Codec
        object Codec : PlayerListItemCodec() {
            override val action = 0
            override fun serialize(obj: PlayerListItem, stream: MinecraftOutputStream) {
                if(obj !is PlayerListItemAdd) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
                stream.writeSignedString(obj.name)
                stream.writeSignedVarInt(obj.properties.size)
                obj.properties.forEach {
                    stream.writeSignedString(it.name)
                    stream.writeSignedString(it.value)
                    stream.writeBoolean(it.signature != null)
                    if (it.signature != null)
                        stream.writeSignedString(it.signature)
                }
                stream.writeSignedVarInt(obj.gamemode)
                stream.writeSignedVarInt(obj.ping)
                stream.writeBoolean(obj.displayName != null)
                if (obj.displayName != null)
                    stream.writeJson(obj.displayName)
            }
            override fun deserialize(stream: MinecraftInputStream): PlayerListItem {
                return PlayerListItemAdd(
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
    class PlayerListItemUpdateGamemode(
            uuid: UUID,
            val gamemode: Int
    ) : PlayerListItem(uuid) {
        override val action = PlayerListItemAdd.Codec.action
        override val codec = PlayerListItemAdd.Codec
        object Codec : PlayerListItemCodec() {
            override val action = 1
            override fun serialize(obj: PlayerListItem, stream: MinecraftOutputStream) {
                if(obj !is PlayerListItemUpdateGamemode) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
                stream.writeSignedVarInt(obj.gamemode)
            }
            override fun deserialize(stream: MinecraftInputStream): PlayerListItemUpdateGamemode {
                return PlayerListItemUpdateGamemode(
                        uuid = stream.readUuid(),
                        gamemode = stream.readSignedVarInt()
                )
            }
        }
    }
    class PlayerListItemUpdateLatency(
            uuid: UUID,
            val ping: Int
    ) : PlayerListItem(uuid) {
        override val action = PlayerListItemAdd.Codec.action
        override val codec = PlayerListItemAdd.Codec
        object Codec : PlayerListItemCodec() {
            override val action = 2
            override fun serialize(obj: PlayerListItem, stream: MinecraftOutputStream) {
                if(obj !is PlayerListItemUpdateLatency) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
                stream.writeSignedVarInt(obj.ping)
            }
            override fun deserialize(stream: MinecraftInputStream): PlayerListItemUpdateLatency {
                return PlayerListItemUpdateLatency(
                        uuid = stream.readUuid(),
                        ping = stream.readSignedVarInt()
                )
            }
        }
    }
    class PlayerListItemUpdateDisplayName(
            uuid: UUID,
            val displayName: String?
    ) : PlayerListItem(uuid) {
        override val action = PlayerListItemAdd.Codec.action
        override val codec = PlayerListItemAdd.Codec
        object Codec : PlayerListItemCodec() {
            override val action = 3
            override fun serialize(obj: PlayerListItem, stream: MinecraftOutputStream) {
                if(obj !is PlayerListItemUpdateDisplayName) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
                if(obj.displayName != null)
                    stream.writeSignedString(obj.displayName)
            }
            override fun deserialize(stream: MinecraftInputStream): PlayerListItemUpdateDisplayName {
                return PlayerListItemUpdateDisplayName(
                        uuid = stream.readUuid(),
                        displayName = if(stream.readBoolean()) stream.readSignedString() else null
                )
            }
        }
    }
    class PlayerListItemRemovePlayer(
            uuid: UUID
    ) : PlayerListItem(uuid) {
        override val action = PlayerListItemAdd.Codec.action
        override val codec = PlayerListItemAdd.Codec
        object Codec : PlayerListItemCodec() {
            override val action = 4
            override fun serialize(obj: PlayerListItem, stream: MinecraftOutputStream) {
                if(obj !is PlayerListItemRemovePlayer) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
            }
            override fun deserialize(stream: MinecraftInputStream): PlayerListItemRemovePlayer {
                return PlayerListItemRemovePlayer(
                        uuid = stream.readUuid()
                )
            }
        }
    }
}