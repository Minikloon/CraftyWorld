package club.kazza.kazzacraft.network.protocol

import club.kazza.kazzacraft.Location
import club.kazza.kazzacraft.metadata.PlayerMetadata
import club.kazza.kazzacraft.network.mojang.ProfileProperty
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import club.kazza.kazzacraft.world.ChunkSection
import io.vertx.core.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.*

abstract class InboundPcPacketList {
    protected abstract fun getCodecs() : List<PcPacket.PcPacketCodec>
    val idToCodec: Map<Int, PcPacket.PcPacketCodec>
    init {
        val mappedPackets = mutableMapOf<Int, PcPacket.PcPacketCodec>()
        for(codec in getCodecs())
            mappedPackets[codec.id] = codec
        idToCodec = mappedPackets
    }
}

object ServerBoundPcHandshakePackets : InboundPcPacketList() {
    override fun getCodecs() = listOf(
            Pc.Client.Handshake.HandshakePcPacket
    )
}

object ServerBoundPcStatusPackets : InboundPcPacketList() {
    override fun getCodecs() = listOf(
            Pc.Client.Status.RequestPcPacket,
            Pc.Client.Status.PingPcPacket
    )

}

object ServerBoundPcLoginPackets : InboundPcPacketList() {
    override fun getCodecs() = listOf(
            Pc.Client.Login.LoginStartPcPacket,
            Pc.Client.Login.EncryptionResponsePcPacket
    )
}

object ServerBoundPcPlayPackets : InboundPcPacketList() {
    override fun getCodecs() = listOf(
            Pc.Client.Play.ClientStatusPcPacket,
            Pc.Client.Play.ClientChatMessagePcPacket,
            Pc.Client.Play.ClientPlayerPosAndLookPcPacket,
            Pc.Client.Play.ClientPluginMessagePcPacket,
            Pc.Client.Play.ClientSettingsPcPacket,
            Pc.Client.Play.PlayerTeleportConfirmPcPacket,
            Pc.Client.Play.ClientKeepAlivePcPacket,
            Pc.Client.Play.ClientPlayerPositionPcPacket,
            Pc.Client.Play.ClientPlayerPosAndLookPcPacket,
            Pc.Client.Play.ClientPlayerLookPcPacket
    )
}


abstract class PcPacket {
    abstract val id: Int
    abstract val codec: PcPacketCodec

    fun serialize(stream: OutputStream) {
        val mcStream = MinecraftOutputStream(stream)
        codec.serialize(this, mcStream)
    }

    abstract class PcPacketCodec {
        abstract val id: Int
        open val expectedSize: Int = 24
        abstract fun serialize(obj: Any, stream: MinecraftOutputStream)
        fun deserialize(stream: InputStream) : PcPacket { return deserialize(MinecraftInputStream(stream)); }
        abstract fun deserialize(stream: MinecraftInputStream) : PcPacket
    }
}

object Pc {
    object Server {
        object Handshake {

        }
        object Status {
            class ResponsePcPacket(
                    val version: ServerVersion,
                    val players: PlayerStatus,
                    val description: Description
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x00
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ResponsePcPacket) throw IllegalArgumentException()
                        stream.writeString(Json.encode(obj))
                    }
                    override fun deserialize(stream: MinecraftInputStream) : PcPacket {
                        val fromJson = Json.decodeValue(stream.readString(), ResponsePcPacket::class.java)
                        return ResponsePcPacket(
                                version = fromJson.version,
                                players = fromJson.players,
                                description = fromJson.description
                        )
                    }
                }
                data class ServerVersion(
                        val name: String,
                        val protocol: Int
                )
                data class PlayerStatus(
                        val max: Int,
                        val online: Int
                )
                data class Description(
                        val text: String
                )
            }
            class PongPcPacket(
                    val epoch: Long
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x01
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is PongPcPacket) throw IllegalArgumentException()
                        stream.writeLong(obj.epoch)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return PongPcPacket(
                                epoch = stream.readLong()
                        )
                    }
                }
            }
        }
        object Login {
            class EncryptionRequestPcPacket(
                    val serverId: String,
                    val publicKey: ByteArray,
                    val verifyToken: ByteArray
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x01
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is EncryptionRequestPcPacket) throw IllegalArgumentException()
                        stream.writeString(obj.serverId)
                        stream.writeUnsignedVarInt(obj.publicKey.size)
                        stream.write(obj.publicKey)
                        stream.writeUnsignedVarInt(obj.verifyToken.size)
                        stream.write(obj.verifyToken)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        val serverId = stream.readString()
                        val publicKey = ByteArray(stream.readUnsignedVarInt())
                        stream.readFully(publicKey)
                        val verifyToken = ByteArray(stream.readUnsignedVarInt())
                        stream.readFully(verifyToken)
                        return EncryptionRequestPcPacket(
                                serverId = serverId,
                                publicKey = publicKey,
                                verifyToken = verifyToken
                        )
                    }
                }
            }
            class LoginSuccessPcPacket(
                    val uuid: UUID,
                    val username: String
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x02
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is LoginSuccessPcPacket) throw IllegalArgumentException()
                        stream.writeString(obj.uuid.toString())
                        stream.writeString(obj.username)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return LoginSuccessPcPacket(
                                uuid = UUID.fromString(stream.readString()),
                                username = stream.readString()
                        )
                    }
                }
            }
            class SetCompressionPcPacket(
                    val maxSize: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x03
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is SetCompressionPcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.maxSize)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return SetCompressionPcPacket(
                                maxSize = stream.readUnsignedVarInt()
                        )
                    }
                }
            }
        }
        object Play {
            class SpawnPlayerPcPacket(
                    val entityId : Int,
                    val playerUuid : UUID,
                    val x: Double,
                    val y: Double,
                    val z: Double,
                    val yaw: Byte,
                    val pitch: Byte,
                    val metadata: PlayerMetadata
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x05
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is SpawnPlayerPcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.entityId)
                        stream.writeUuid(obj.playerUuid)
                        stream.writeDouble(obj.x)
                        stream.writeDouble(obj.y)
                        stream.writeDouble(obj.z)
                        stream.writeByte(obj.yaw)
                        stream.writeByte(obj.pitch)
                        obj.metadata.writeToStream(stream)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        throw NotImplementedError() // TODO
                        /*
                        return SpawnPlayerPcPacket(
                                entityId = stream.readUnsignedVarInt(),
                                playerUuid = stream.readUuid(),
                                x = stream.readDouble(),
                                y = stream.readDouble(),
                                z = stream.readDouble(),
                                yaw = stream.readByte(),
                                pitch = stream.readByte(),
                                metadata = stream.readBytes(stream.available())
                        )
                        */
                    }
                }
            }
            class ServerDifficultyPcPacket(
                    val difficulty: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x0D
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ServerDifficultyPcPacket) throw IllegalArgumentException()
                        stream.writeByte(obj.difficulty)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ServerDifficultyPcPacket(
                                difficulty = stream.readUnsignedByte()
                        )
                    }
                }
            }
            class ServerChatMessage(
                    val chat: McChat,
                    val position: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x0F
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ServerChatMessage) throw IllegalArgumentException()
                        stream.writeJson(obj.chat)
                        stream.writeByte(obj.position)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ServerChatMessage(
                                chat = Json.decodeValue(stream.readString(), McChat::class.java),
                                position = stream.readByte().toInt()
                        )
                    }
                }
            }
            class ServerPluginMessagePcPacket(
                    val channel: String,
                    val data: ByteArray
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x18
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ServerPluginMessagePcPacket) throw IllegalArgumentException()
                        stream.writeString(obj.channel)
                        stream.write(obj.data)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ServerPluginMessagePcPacket(
                                channel = stream.readString(),
                                data = stream.readBytes(stream.available())
                        )
                    }
                }
            }
            class ServerKeepAlivePcPacket(
                    val confirmId: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x1F
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ServerKeepAlivePcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.confirmId)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ServerKeepAlivePcPacket(
                                confirmId = stream.readUnsignedVarInt()
                        )
                    }
                }
            }
            class ChunkDataPcPacket(
                    val x: Int,
                    val z: Int,
                    val continuous: Boolean,
                    val chunkMask: Int,
                    val sections: Array<ChunkSection?>,
                    val biomes: ByteArray?
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x20
                    override val expectedSize = 180000
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ChunkDataPcPacket) throw IllegalArgumentException()
                        stream.writeInt(obj.x)
                        stream.writeInt(obj.z)
                        stream.writeBoolean(obj.continuous)
                        stream.writeUnsignedVarInt(obj.chunkMask)
                        val sections = obj.sections.filterNotNull()
                        val dataSize = sections.sumBy { it.byteSize } + if(obj.biomes == null) 0 else obj.biomes.size
                        stream.writeUnsignedVarInt(dataSize)
                        sections.forEach {
                            it.writeToStream(stream)
                        }
                        if(obj.biomes != null)
                            stream.write(obj.biomes)
                        stream.writeUnsignedVarInt(0)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        throw NotImplementedError()
                    }
                }
            }
            class JoinGamePcPacket(
                    val eid: Int,
                    val gamemode: Int,
                    val dimension: Int,
                    val difficulty: Int,
                    val maxPlayers: Int,
                    val levelType: String,
                    val reducedDebug: Boolean
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x23
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is JoinGamePcPacket) throw IllegalArgumentException()
                        stream.writeInt(obj.eid)
                        stream.writeByte(obj.gamemode)
                        stream.writeInt(obj.dimension)
                        stream.writeByte(obj.difficulty)
                        stream.writeByte(obj.maxPlayers)
                        stream.writeString(obj.levelType)
                        stream.writeBoolean(obj.reducedDebug)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return JoinGamePcPacket(
                                eid = stream.readInt(),
                                gamemode = stream.readUnsignedByte(),
                                dimension = stream.readInt(),
                                difficulty = stream.readUnsignedByte(),
                                maxPlayers = stream.readUnsignedByte(),
                                levelType = stream.readString(),
                                reducedDebug = stream.readBoolean()
                        )
                    }
                }
            }
            class PlayerAbilitiesPcPacket(
                    val flags: Int,
                    val flyingSpeed: Float,
                    val fovModifier: Float
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x2B
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is PlayerAbilitiesPcPacket) throw IllegalArgumentException()
                        stream.writeByte(obj.flags)
                        stream.writeFloat(obj.flyingSpeed)
                        stream.writeFloat(obj.fovModifier)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return PlayerAbilitiesPcPacket(
                                flags = stream.readInt(),
                                flyingSpeed = stream.readFloat(),
                                fovModifier = stream.readFloat()
                        )
                    }
                }
            }
            class PlayerListItemPcPacket(
                    val action: Int,
                    val items: List<PlayerListItem>
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x2D
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is PlayerListItemPcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.action)
                        stream.writeUnsignedVarInt(obj.items.size)
                        obj.items.forEach { it.serialize(stream) }
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
                            if(obj !is PlayerListItemAdd) throw IllegalArgumentException()
                            stream.writeUuid(obj.uuid)
                            stream.writeString(obj.name)
                            stream.writeUnsignedVarInt(obj.properties.size)
                            obj.properties.forEach {
                                stream.writeString(it.name)
                                stream.writeString(it.value)
                                stream.writeBoolean(it.signature != null)
                                if(it.signature != null)
                                    stream.writeString(it.signature)
                            }
                            stream.writeUnsignedVarInt(obj.gamemode)
                            stream.writeUnsignedVarInt(obj.ping)
                            stream.writeBoolean(obj.displayName != null)
                            if(obj.displayName != null)
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
                                                signature = if(stream.readBoolean()) stream.readString() else null
                                        )
                                    },
                                    gamemode = stream.readUnsignedVarInt(),
                                    ping = stream.readUnsignedVarInt(),
                                    displayName = if(stream.readBoolean()) stream.readJson(McChat::class.java) else null
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
            class PlayerTeleportPcPacket(
                    val loc: Location,
                    val relativeFlags: Int,
                    val confirmId: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x2E
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is PlayerTeleportPcPacket) throw IllegalArgumentException()
                        stream.writeDouble(obj.loc.x)
                        stream.writeDouble(obj.loc.y)
                        stream.writeDouble(obj.loc.z)
                        stream.writeFloat(obj.loc.yaw)
                        stream.writeFloat(obj.loc.pitch)
                        stream.writeByte(obj.relativeFlags)
                        stream.writeUnsignedVarInt(obj.confirmId)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return PlayerTeleportPcPacket(
                                loc = Location(
                                        x = stream.readDouble(),
                                        y = stream.readDouble(),
                                        z = stream.readDouble(),
                                        yaw = stream.readFloat(),
                                        pitch = stream.readFloat()
                                ),
                                relativeFlags = stream.readByte().toInt(),
                                confirmId = stream.readUnsignedVarInt()
                        )
                    }
                }
            }
            class SpawnPositionPcPacket(
                    val loc: Location
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x43
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is SpawnPositionPcPacket) throw IllegalArgumentException()
                        stream.writeBlockLocation(obj.loc)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return SpawnPositionPcPacket(
                                loc = stream.readBlockLocation()
                        )
                    }
                }
            }
            class PlayerListHeaderFooterPcPacket(
                    val header: McChat,
                    val footer: McChat
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x47
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is PlayerListHeaderFooterPcPacket) throw IllegalArgumentException()
                        stream.writeJson(obj.header)
                        stream.writeJson(obj.footer)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return PlayerListHeaderFooterPcPacket(
                                header = Json.decodeValue(stream.readString(), McChat::class.java),
                                footer = Json.decodeValue(stream.readString(), McChat::class.java)
                        )
                    }
                }
            }
            class EntityTeleportPcPacket(
                    val entityId: Int,
                    val x: Double,
                    val y: Double,
                    val z: Double,
                    val yaw: Byte,
                    val pitch: Byte,
                    val onGround: Boolean
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x49
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is EntityTeleportPcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.entityId)
                        stream.writeDouble(obj.x)
                        stream.writeDouble(obj.y)
                        stream.writeDouble(obj.z)
                        stream.writeByte(obj.yaw)
                        stream.writeByte(obj.pitch)
                        stream.writeBoolean(obj.onGround)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return EntityTeleportPcPacket(
                                entityId = stream.readUnsignedVarInt(),
                                x = stream.readDouble(),
                                y = stream.readDouble(),
                                z = stream.readDouble(),
                                yaw = stream.readByte(),
                                pitch = stream.readByte(),
                                onGround = stream.readBoolean()
                        )
                    }
                }
            }
        }
    }

    object Client {
        object Handshake {
            class HandshakePcPacket(
                    val protocolVersion: Int,
                    val serverAddress: String,
                    val port: Int,
                    val nextState: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x00
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is HandshakePcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.protocolVersion)
                        stream.writeString(obj.serverAddress)
                        stream.writeShort(obj.port)
                        stream.writeUnsignedVarInt(obj.nextState)
                    }
                    override fun deserialize(stream: MinecraftInputStream) : PcPacket {
                        return HandshakePcPacket(
                                protocolVersion = stream.readUnsignedVarInt(),
                                serverAddress = stream.readString(),
                                port = stream.readUnsignedShort(),
                                nextState = stream.readUnsignedVarInt()
                        )
                    }
                }
            }
        }
        object Status {
            class RequestPcPacket(

            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x00
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is RequestPcPacket) throw IllegalArgumentException()
                    }
                    override fun deserialize(stream: MinecraftInputStream) : PcPacket {
                        return RequestPcPacket()
                    }
                }
            }
            class PingPcPacket(
                    val epoch: Long
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x01
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is PingPcPacket) throw IllegalArgumentException()
                        stream.writeLong(obj.epoch)
                    }
                    override fun deserialize(stream: MinecraftInputStream) : PcPacket {
                        return PingPcPacket(
                                epoch = stream.readLong()
                        )
                    }
                }
            }
        }
        object Login {
            class LoginStartPcPacket(
                    val username: String
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x00
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is LoginStartPcPacket) throw IllegalArgumentException()
                        stream.writeString(obj.username)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return LoginStartPcPacket(
                                username = stream.readString()
                        )
                    }
                }
            }
            class EncryptionResponsePcPacket(
                    val sharedSecret: ByteArray,
                    val verifyToken: ByteArray
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x01
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is EncryptionResponsePcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.sharedSecret.size)
                        stream.write(obj.sharedSecret)
                        stream.writeUnsignedVarInt(obj.verifyToken.size)
                        stream.write(obj.verifyToken)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        val sharedSecret = ByteArray(stream.readUnsignedVarInt())
                        stream.readFully(sharedSecret)
                        val verifyToken = ByteArray(stream.readUnsignedVarInt())
                        stream.readFully(verifyToken)
                        return EncryptionResponsePcPacket(
                                sharedSecret = sharedSecret,
                                verifyToken = verifyToken
                        )
                    }
                }
            }
        }
        object Play {
            class PlayerTeleportConfirmPcPacket(
                    val confirmId: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x00
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is PlayerTeleportConfirmPcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.confirmId)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return PlayerTeleportConfirmPcPacket(
                                confirmId = stream.readUnsignedVarInt()
                        )
                    }
                }
            }
            class ClientChatMessagePcPacket(
                    val message: String
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x02
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ClientChatMessagePcPacket) throw IllegalArgumentException()
                        stream.writeString(obj.message)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientChatMessagePcPacket(
                                message = stream.readString()
                        )
                    }
                }
            }
            class ClientStatusPcPacket(
                    val action: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x03
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ClientStatusPcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.action)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientStatusPcPacket(
                                action = stream.readUnsignedVarInt()
                        )
                    }
                }
            }
            class ClientSettingsPcPacket(
                    val locale: String,
                    val viewDistance: Int,
                    val chatMode: Int,
                    val chatColors: Boolean,
                    val skinParts: Int,
                    val mainHand: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x04
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ClientSettingsPcPacket) throw IllegalArgumentException()
                        stream.writeString(obj.locale)
                        stream.writeByte(obj.viewDistance)
                        stream.writeUnsignedVarInt(obj.chatMode)
                        stream.writeBoolean(obj.chatColors)
                        stream.writeByte(obj.skinParts)
                        stream.writeUnsignedVarInt(obj.mainHand)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientSettingsPcPacket(
                                locale = stream.readString(),
                                viewDistance = stream.readByte().toInt(),
                                chatMode = stream.readUnsignedVarInt(),
                                chatColors = stream.readBoolean(),
                                skinParts = stream.readUnsignedByte(),
                                mainHand = stream.readUnsignedVarInt()
                        )
                    }
                }
            }
            class ClientPluginMessagePcPacket(
                    val channel: String,
                    val data: ByteArray
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x09
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ClientPluginMessagePcPacket) throw IllegalArgumentException()
                        stream.writeString(obj.channel)
                        stream.write(obj.data)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientPluginMessagePcPacket(
                                channel = stream.readString(),
                                data = stream.readBytes(stream.available())
                        )
                    }
                }
            }
            class ClientKeepAlivePcPacket(
                    val confirmId: Int
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x0B
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ClientKeepAlivePcPacket) throw IllegalArgumentException()
                        stream.writeUnsignedVarInt(obj.confirmId)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientKeepAlivePcPacket(
                                confirmId = stream.readUnsignedVarInt()
                        )
                    }
                }
            }
            class ClientPlayerPositionPcPacket(
                    val x: Double,
                    val y: Double,
                    val z: Double,
                    val onGround: Boolean
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x0C
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ClientPlayerPositionPcPacket) throw IllegalArgumentException()
                        stream.writeDouble(obj.x)
                        stream.writeDouble(obj.y)
                        stream.writeDouble(obj.z)
                        stream.writeBoolean(obj.onGround)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientPlayerPositionPcPacket(
                                x = stream.readDouble(),
                                y = stream.readDouble(),
                                z = stream.readDouble(),
                                onGround = stream.readBoolean()
                        )
                    }
                }
            }
            class ClientPlayerPosAndLookPcPacket(
                    val x: Double,
                    val y: Double,
                    val z: Double,
                    val yaw: Float,
                    val pitch: Float,
                    val onGround: Boolean
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x0D
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ClientPlayerPosAndLookPcPacket) throw IllegalArgumentException()
                        stream.writeDouble(obj.x)
                        stream.writeDouble(obj.y)
                        stream.writeDouble(obj.z)
                        stream.writeFloat(obj.yaw)
                        stream.writeFloat(obj.pitch)
                        stream.writeBoolean(obj.onGround)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientPlayerPosAndLookPcPacket(
                                x = stream.readDouble(),
                                y = stream.readDouble(),
                                z = stream.readDouble(),
                                yaw = stream.readFloat(),
                                pitch = stream.readFloat(),
                                onGround = stream.readBoolean()
                        )
                    }
                }
            }
            class ClientPlayerLookPcPacket(
                    val yaw: Float,
                    val pitch: Float,
                    val onGround: Boolean
            ) : PcPacket() {
                override val id = Codec.id
                override val codec = Codec
                companion object Codec : PcPacketCodec() {
                    override val id = 0x0E
                    override fun serialize(obj: Any, stream: MinecraftOutputStream) {
                        if(obj !is ClientPlayerLookPcPacket) throw IllegalArgumentException()
                        stream.writeFloat(obj.yaw)
                        stream.writeFloat(obj.pitch)
                        stream.writeBoolean(obj.onGround)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientPlayerLookPcPacket(
                                yaw = stream.readFloat(),
                                pitch = stream.readFloat(),
                                onGround = stream.readBoolean()
                        )
                    }
                }
            }
        }
    }
}