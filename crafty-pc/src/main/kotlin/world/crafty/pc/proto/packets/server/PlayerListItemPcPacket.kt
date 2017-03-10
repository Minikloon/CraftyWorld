package world.crafty.pc.proto.packets.server

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
            stream.writeUnsignedVarInt(obj.action)
            stream.writeUnsignedVarInt(obj.items.size)
            obj.items.forEach { it.serializeNoHeader(stream) }
        }

        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            val action = stream.readUnsignedVarInt()
            val actionCodec = actionCodecs[action] ?: throw IllegalStateException("Unknown player list action id $action")
            val items = (0 until stream.readUnsignedVarInt()).map {
                actionCodec.deserialize(stream) as PlayerListItem
            }
            return PlayerListItemPcPacket(action, items)
        }

        val actionCodecs = mapOf(
                PlayerListItemAdd.id to PlayerListItemAdd
        )
    }

    abstract class PlayerListItem(val uuid: UUID) : PcPacket() // TODO: Don't use PcPacket
    class PlayerListItemAdd(
            uuid: UUID,
            val name: String,
            val properties: List<ProfileProperty>,
            val gamemode: Int,
            val ping: Int,
            val displayName: McChat? = null
    ) : PlayerListItem(uuid) {
        override val id = PlayerListItemAddCodec.id
        override val codec = PlayerListItemAddCodec

        companion object PlayerListItemAddCodec : PcPacketCodec() {
            override val id = 0
            override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                if (obj !is PlayerListItemAdd) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
                stream.writeString(obj.name)
                stream.writeUnsignedVarInt(obj.properties.size)
                obj.properties.forEach {
                    stream.writeString(it.name)
                    stream.writeString(it.value)
                    stream.writeBoolean(it.signature != null)
                    if (it.signature != null)
                        stream.writeString(it.signature)
                }
                stream.writeUnsignedVarInt(obj.gamemode)
                stream.writeUnsignedVarInt(obj.ping)
                stream.writeBoolean(obj.displayName != null)
                if (obj.displayName != null)
                    stream.writeJson(obj.displayName)
            }

            override fun deserialize(stream: MinecraftInputStream): PcPacket {
                return PlayerListItemAdd(
                        uuid = stream.readUuid(),
                        name = stream.readString(),
                        properties = (0 until stream.readUnsignedVarInt()).map {
                            ProfileProperty(
                                    name = stream.readString(),
                                    value = stream.readString(),
                                    signature = if (stream.readBoolean()) stream.readString() else null
                            )
                        },
                        gamemode = stream.readUnsignedVarInt(),
                        ping = stream.readUnsignedVarInt(),
                        displayName = if (stream.readBoolean()) stream.readJson(McChat::class.java) else null
                )
            }
        }
    }
    class PlayerListItemUpdateGamemode(
            uuid: UUID,
            val gamemode: Int
    ) : PlayerListItem(uuid) {
        override val id = PlayerListItemUpdateGamemodeCodec.id
        override val codec = PlayerListItemUpdateGamemodeCodec
        companion object PlayerListItemUpdateGamemodeCodec : PcPacketCodec() {
            override val id = 1
            override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                if(obj !is PlayerListItemUpdateGamemode) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
                stream.writeUnsignedVarInt(obj.gamemode)
            }
            override fun deserialize(stream: MinecraftInputStream): PcPacket {
                return PlayerListItemUpdateGamemode(
                        uuid = stream.readUuid(),
                        gamemode = stream.readUnsignedVarInt()
                )
            }
        }
    }
    class PlayerListItemUpdateLatency(
            uuid: UUID,
            val ping: Int
    ) : PlayerListItem(uuid) {
        override val id = PlayerListItemUpdateLatencyCodec.id
        override val codec = PlayerListItemUpdateLatencyCodec
        companion object PlayerListItemUpdateLatencyCodec : PcPacketCodec() {
            override val id = 2
            override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                if(obj !is PlayerListItemUpdateLatency) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
                stream.writeUnsignedVarInt(obj.ping)
            }
            override fun deserialize(stream: MinecraftInputStream): PcPacket {
                return PlayerListItemUpdateLatency(
                        uuid = stream.readUuid(),
                        ping = stream.readUnsignedVarInt()
                )
            }
        }
    }
    class PlayerListItemUpdateDisplayName(
            uuid: UUID,
            val displayName: String?
    ) : PlayerListItem(uuid) {
        override val id = PlayerListItemUpdateDisplayNameCodec.id
        override val codec = PlayerListItemUpdateDisplayNameCodec
        companion object PlayerListItemUpdateDisplayNameCodec : PcPacketCodec() {
            override val id = 3
            override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                if(obj !is PlayerListItemUpdateDisplayName) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
                if(obj.displayName != null)
                    stream.writeString(obj.displayName)
            }
            override fun deserialize(stream: MinecraftInputStream): PcPacket {
                return PlayerListItemUpdateDisplayName(
                        uuid = stream.readUuid(),
                        displayName = if(stream.readBoolean()) stream.readString() else null
                )
            }
        }
    }
    class PlayerListItemRemovePlayer(
            uuid: UUID
    ) : PlayerListItem(uuid) {
        override val id = PlayerListItemRemovePlayerCodec.id
        override val codec = PlayerListItemRemovePlayerCodec
        companion object PlayerListItemRemovePlayerCodec : PcPacketCodec() {
            override val id = 4
            override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                if(obj !is PlayerListItemRemovePlayer) throw IllegalArgumentException()
                stream.writeUuid(obj.uuid)
            }
            override fun deserialize(stream: MinecraftInputStream): PcPacket {
                return PlayerListItemRemovePlayer(
                        uuid = stream.readUuid()
                )
            }
        }
    }
}