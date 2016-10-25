package club.kazza.kazzacraft.network.protocol

import club.kazza.kazzacraft.Location
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import club.kazza.kazzacraft.world.ChunkSection
import io.vertx.core.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.*

abstract class InboundPacketList {
    protected abstract fun getCodecs() : List<PcPacket.PcPacketCodec>
    val idToCodec: Map<Int, PcPacket.PcPacketCodec>
    init {
        val mappedPackets = mutableMapOf<Int, PcPacket.PcPacketCodec>()
        for(codec in getCodecs())
            mappedPackets[codec.id] = codec
        idToCodec = mappedPackets
    }
}

object ServerBoundPcHandshakePackets : InboundPacketList() {
    override fun getCodecs() = listOf(
            Pc.Client.Handshake.HandshakePcPacket
    )
}

object ServerBoundPcStatusPackets : InboundPacketList() {
    override fun getCodecs() = listOf(
            Pc.Client.Status.RequestPcPacket,
            Pc.Client.Status.PingPcPacket
    )

}

object ServerBoundPcLoginPackets : InboundPacketList() {
    override fun getCodecs() = listOf(
            Pc.Client.Login.LoginStartPcPacket,
            Pc.Client.Login.EncryptionResponsePcPacket
    )
}

object ServerBoundPcPlayPackets : InboundPacketList() {
    override fun getCodecs() = listOf(
            Pc.Client.Play.ClientStatusPcPacket,
            Pc.Client.Play.ClientChatMessagePcPacket,
            Pc.Client.Play.PlayerPosAndLook,
            Pc.Client.Play.ClientPluginMessagePcPacket,
            Pc.Client.Play.ClientSettingsPcPacket,
            Pc.Client.Play.PlayerTeleportConfirmPcPacket,
            Pc.Client.Play.ClientKeepAlivePcPacket
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
                        stream.writeVarInt(obj.publicKey.size)
                        stream.write(obj.publicKey)
                        stream.writeVarInt(obj.verifyToken.size)
                        stream.write(obj.verifyToken)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        val serverId = stream.readString()
                        val publicKey = ByteArray(stream.readVarInt())
                        stream.read(publicKey)
                        val verifyToken = ByteArray(stream.readVarInt())
                        stream.read(verifyToken)
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
                        stream.writeVarInt(obj.maxSize)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return SetCompressionPcPacket(
                                maxSize = stream.readVarInt()
                        )
                    }
                }
            }
        }
        object Play {
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
                        val json = Json.encode(obj.chat)
                        stream.writeString(json)
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
                        stream.writeVarInt(obj.confirmId)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ServerKeepAlivePcPacket(
                                confirmId = stream.readVarInt()
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
                        stream.writeVarInt(obj.chunkMask)
                        val sections = obj.sections.filterNotNull()
                        val dataSize = sections.sumBy { it.byteSize } + if(obj.biomes == null) 0 else obj.biomes.size
                        stream.writeVarInt(dataSize)
                        sections.forEach {
                            it.writeToStream(stream)
                        }
                        if(obj.biomes != null)
                            stream.write(obj.biomes)
                        stream.writeVarInt(0)
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
                        stream.writeVarInt(obj.confirmId)
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
                                confirmId = stream.readVarInt()
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
                        stream.writeVarInt(obj.protocolVersion)
                        stream.writeString(obj.serverAddress)
                        stream.writeShort(obj.port)
                        stream.writeVarInt(obj.nextState)
                    }
                    override fun deserialize(stream: MinecraftInputStream) : PcPacket {
                        return HandshakePcPacket(
                                protocolVersion = stream.readVarInt(),
                                serverAddress = stream.readString(),
                                port = stream.readUnsignedShort(),
                                nextState = stream.readVarInt()
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
                        stream.writeVarInt(obj.sharedSecret.size)
                        stream.write(obj.sharedSecret)
                        stream.writeVarInt(obj.verifyToken.size)
                        stream.write(obj.verifyToken)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        val sharedSecret = ByteArray(stream.readVarInt())
                        stream.read(sharedSecret)
                        val verifyToken = ByteArray(stream.readVarInt())
                        stream.read(verifyToken)
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
                        stream.writeVarInt(obj.confirmId)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return PlayerTeleportConfirmPcPacket(
                                confirmId = stream.readVarInt()
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
                        stream.writeVarInt(obj.action)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientStatusPcPacket(
                                action = stream.readVarInt()
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
                        stream.writeVarInt(obj.chatMode)
                        stream.writeBoolean(obj.chatColors)
                        stream.writeByte(obj.skinParts)
                        stream.writeVarInt(obj.mainHand)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientSettingsPcPacket(
                                locale = stream.readString(),
                                viewDistance = stream.readByte().toInt(),
                                chatMode = stream.readVarInt(),
                                chatColors = stream.readBoolean(),
                                skinParts = stream.readUnsignedByte(),
                                mainHand = stream.readVarInt()
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
                        stream.writeVarInt(obj.confirmId)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return ClientKeepAlivePcPacket(
                                confirmId = stream.readVarInt()
                        )
                    }
                }
            }
            class PlayerPosAndLook(
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
                        if(obj !is PlayerPosAndLook) throw IllegalArgumentException()
                        stream.writeDouble(obj.x)
                        stream.writeDouble(obj.y)
                        stream.writeDouble(obj.z)
                        stream.writeFloat(obj.yaw)
                        stream.writeFloat(obj.pitch)
                        stream.writeBoolean(obj.onGround)
                    }
                    override fun deserialize(stream: MinecraftInputStream): PcPacket {
                        return PlayerPosAndLook(
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
        }
    }
}