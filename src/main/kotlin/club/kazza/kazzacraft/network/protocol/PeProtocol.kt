package club.kazza.kazzacraft.network.protocol

import club.kazza.kazzacraft.network.protocol.jwt.PeJwt
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import club.kazza.kazzacraft.utils.CompressionAlgorithm
import club.kazza.kazzacraft.utils.compress
import club.kazza.kazzacraft.utils.decompress
import club.kazza.kazzacraft.utils.toHexStr
import io.vertx.core.json.JsonObject
import io.vertx.core.net.SocketAddress
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.zip.Deflater

abstract class InboundPePacketList {
    protected abstract fun getCodecs() : List<PePacket.PePacketCodec>
    val idToCodec: Map<Int, PePacket.PePacketCodec>
    init {
        val mappedPackets = mutableMapOf<Int, PePacket.PePacketCodec>()
        for(codec in getCodecs()) {
            val prev = mappedPackets.put(codec.id, codec)
            if(prev != null)
                throw IllegalStateException("${codec::class.qualifiedName} registered twice in ${this::class.simpleName}")
        }
        idToCodec = mappedPackets
    }
}

object ServerBoundPeRaknetPackets : InboundPePacketList() {
    override fun getCodecs(): List<PePacket.PePacketCodec> {
        return listOf(
                UnconnectedPingClientPePacket.Codec,
                OpenConnectionRequest1PePacket.Codec,
                OpenConnectionRequest2PePacket.Codec
        )
    }
}

object ServerBoundPeTopLevelPackets : InboundPePacketList() {
    override fun getCodecs(): List<PePacket.PePacketCodec> {
        return listOf(
                ConnectionRequestPePacket.Codec,
                NewIncomingConnection.Codec,
                ConnectedPingPePacket.Codec,
                EncryptionWrapperPePacket.Codec,
                LoginPePacket.Codec,
                CompressionWrapperPePacket.Codec
        )
    }
}

object ServerBoundPeWrappedPackets : InboundPePacketList() {
    override fun getCodecs(): List<PePacket.PePacketCodec> {
        return listOf(
                ResourcePackClientResponsePePacket.Codec,
                ChunkRadiusRequestPePacket.Codec,
                PlayerActionPePacket.Codec,
                SetPlayerPositionPePacket.Codec
        )
    }
}

interface PeCodec<T> {
    fun serialize(obj: T, stream: MinecraftOutputStream)
    fun deserialize(stream: MinecraftInputStream) : T
}

abstract class PePacket {
    abstract val id: Int
    abstract val codec: PePacketCodec

    fun serialize(stream: OutputStream) {
        val mcStream = MinecraftOutputStream(stream)
        codec.serialize(this, mcStream)
    }
    
    fun serialized() : ByteArray {
        val bs = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(bs)
        serialize(mcStream)
        return bs.toByteArray()
    }
    
    fun serializedWithId() : ByteArray {
        val bs = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(bs)
        mcStream.writeByte(id)
        serialize(mcStream)
        return bs.toByteArray()
    }

    abstract class PePacketCodec {
        abstract val id: Int
        abstract fun serialize(obj: Any, stream: MinecraftOutputStream)
        fun deserialize(stream: InputStream) : PePacket { return deserialize(MinecraftInputStream(stream)); }
        abstract fun deserialize(stream: MinecraftInputStream) : PePacket
    }
}

private val unconnectedBlabber: ByteArray = listOf(0x00, 0xff, 0xff, 0x00, 0xfe, 0xfe, 0xfe, 0xfe, 0xfd, 0xfd, 0xfd, 0xfd, 0x12, 0x34, 0x56, 0x78).map(Int::toByte).toByteArray()

class ConnectedPingPePacket(
        val pingTimestamp: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x00
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ConnectedPingPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.pingTimestamp)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ConnectedPingPePacket(
                    pingTimestamp = stream.readLong()
            )
        }
    }
}

class LoginPePacket(
        val protocolVersion: Int,
        val edition: Int,
        val certChain: List<PeJwt>,
        val skinJwt: String
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x01
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is LoginPePacket) throw IllegalArgumentException()
            stream.writeInt(obj.protocolVersion)
            stream.writeByte(obj.edition)
            val payload = obj.certChain.toString().toByteArray() + obj.skinJwt.toString().toByteArray()
            stream.write(payload.compress(CompressionAlgorithm.ZLIB))
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val protocolVersion = stream.readInt()
            val edition = stream.readByte().toInt()
            
            val payloadSize = stream.readUnsignedVarInt()
            val zlibedPayload = stream.readRemainingBytes()
            
            val payload = zlibedPayload.decompress(CompressionAlgorithm.ZLIB, payloadSize)
            val pStream = MinecraftInputStream(payload)
            
            val chainStr = pStream.readString(pStream.readIntLe())
            val chainJson = JsonObject(chainStr)
            val certChain = chainJson.getJsonArray("chain").map { PeJwt.parse(it as String) }
            
            val skinJwt = pStream.readString(pStream.readIntLe())
            
            return LoginPePacket(
                    protocolVersion = protocolVersion,
                    edition = edition,
                    certChain = certChain,
                    skinJwt = skinJwt
            )
        }
    }
}

enum class PlayerStatus { LOGIN_ACCEPTED, LOGIN_FAILED_CLIENT, LOGIN_FAILED_SERVER, SPAWN }
class PlayerStatusPePacket(
        val status: PlayerStatus
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x02
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerStatusPePacket) throw IllegalStateException()
            stream.writeInt(obj.status.ordinal)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return PlayerStatusPePacket(
                    status = PlayerStatus.values()[stream.readInt()]
            )
        }
    }
}

class ConnectedPongPePacket(
        val pingTimestamp: Long,
        val pongTimestamp: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x03
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ConnectedPongPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.pingTimestamp)
            stream.writeLong(obj.pongTimestamp)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ConnectedPongPePacket(
                    pingTimestamp = stream.readLong(),
                    pongTimestamp = stream.readLong()
            )
        }
    }
}

class ResourcePackInfo(
        val id: String,
        val version: ResourcePackVersion
) {
    object Codec : PeCodec<ResourcePackInfo> {
        override fun serialize(obj: ResourcePackInfo, stream: MinecraftOutputStream) {
            stream.writeString(obj.id)
            ResourcePackVersion.Codec.serialize(obj.version, stream)
        }
        override fun deserialize(stream: MinecraftInputStream): ResourcePackInfo {
            return ResourcePackInfo(
                    id = stream.readString(),
                    version = ResourcePackVersion.Codec.deserialize(stream)
            )
        }
    }
}
class ResourcePackVersion(
        val version: String,
        val unknown: Long
) {
    object Codec : PeCodec<ResourcePackVersion> {
        override fun serialize(obj: ResourcePackVersion, stream: MinecraftOutputStream) {
            stream.writeString(obj.version)
            stream.writeLong(obj.unknown)
        }
        override fun deserialize(stream: MinecraftInputStream): ResourcePackVersion {
            return ResourcePackVersion(
                    version = stream.readString(),
                    unknown = stream.readLong()
            )
        }
    }
}
class ResourcePackTriggerPePacket(
        val mustAccept: Boolean,
        val behaviors: List<ResourcePackInfo>,
        val resources: List<ResourcePackInfo>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x07
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ResourcePackTriggerPePacket) throw IllegalArgumentException()
            stream.writeBoolean(obj.mustAccept)
            stream.writeShort(obj.behaviors.size)
            obj.behaviors.forEach { ResourcePackInfo.Codec.serialize(it, stream) }
            stream.writeShort(obj.resources.size)
            obj.resources.forEach { ResourcePackInfo.Codec.serialize(it, stream) }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ResourcePackTriggerPePacket(
                    mustAccept = stream.readBoolean(),
                    behaviors = (1..stream.readShort()).map { ResourcePackInfo.Codec.deserialize(stream) },
                    resources = (1..stream.readShort()).map { ResourcePackInfo.Codec.deserialize(stream) }
            )
        }
    }
}

class ResourcePackDataPePacket(
        val mustAccept: Boolean,
        val behaviorVersions: List<ResourcePackVersion>,
        val resourceVersions: List<ResourcePackVersion>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x08
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ResourcePackDataPePacket) throw IllegalArgumentException()
            stream.writeBoolean(obj.mustAccept)
            stream.writeShort(obj.behaviorVersions.size)
            obj.behaviorVersions.forEach { ResourcePackVersion.Codec.serialize(it, stream) }
            stream.writeShort(obj.resourceVersions.size)
            obj.resourceVersions.forEach { ResourcePackVersion.Codec.serialize(it, stream) }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ResourcePackDataPePacket(
                    mustAccept = stream.readBoolean(),
                    behaviorVersions = (1..stream.readShort()).map { ResourcePackVersion.Codec.deserialize(stream) },
                    resourceVersions = (1..stream.readShort()).map { ResourcePackVersion.Codec.deserialize(stream) }
            )
        }
    }
}

enum class ResourcePackClientStatus { UNKNOWN, UNKNOWN_2, REQUEST_INFO, REQUEST_DATA, PLAYER_READY }
class ResourcePackClientResponsePePacket(
        val status: ResourcePackClientStatus,
        val versions: List<ResourcePackVersion>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x09
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ResourcePackClientResponsePePacket) throw IllegalArgumentException()
            stream.writeByte(obj.status.ordinal)
            stream.writeShort(obj.versions.size)
            obj.versions.forEach { ResourcePackVersion.Codec.serialize(it, stream) }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ResourcePackClientResponsePePacket(
                    status = ResourcePackClientStatus.values()[stream.readByte().toInt()],
                    versions = (1..stream.readShort()).map { ResourcePackVersion.Codec.deserialize(stream) }
            )
        }
    }
}

class NewIncomingConnection(
        val clientEndpoint: SocketAddress,
        val addresses: List<SocketAddress>,
        val incomingTimestamp: Long,
        val serverTimestamp: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x13
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is NewIncomingConnection) throw IllegalArgumentException()
            stream.writeAddress(obj.clientEndpoint)
            obj.addresses.forEach { stream.writeAddress(it) }
            stream.writeLong(obj.incomingTimestamp)
            stream.writeLong(obj.serverTimestamp)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return NewIncomingConnection(
                    clientEndpoint = stream.readAddress(),
                    addresses = (1..10).map { stream.readAddress() },
                    incomingTimestamp = stream.readLong(),
                    serverTimestamp = stream.readLong()
            )
        }
    }
}

class AckPePacket(
        val datagramSeqNos: List<Int>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0xc0
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AckPePacket) throw IllegalArgumentException()
            val ranges = asRanges(obj.datagramSeqNos)
            stream.writeShort(ranges.count())
            ranges.forEach {
                val startEqualsEnd = it.first == it.last
                stream.writeBoolean(startEqualsEnd)
                if(startEqualsEnd) {
                    stream.write3BytesInt(it.first)
                }
                else {
                    stream.write3BytesInt(it.first)
                    stream.write3BytesInt(it.last)
                }
            }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val rangesCount = stream.readUnsignedShort()
            val ranges = ArrayList<IntRange>(rangesCount)
            repeat(rangesCount) {
                val startEqualsEnd = stream.readBoolean()
                if(startEqualsEnd) {
                    val datagramSeqNo = stream.read3BytesInt()
                    ranges.add(datagramSeqNo..datagramSeqNo)
                }
                else {
                    val first = stream.read3BytesInt()
                    val last = stream.read3BytesInt()
                    ranges.add(first..last)
                }
            }
            val seqNos = ranges.flatMap { it.asIterable() }
            return AckPePacket(seqNos)
        }
        private fun asRanges(ints: List<Int>) : List<IntRange> {
            var count = 0
            return ints
                    .groupBy { i -> count++ - i }.values
                    .map { it.first()..it.last() }
        }
    }
}

class UnconnectedPingClientPePacket(
        val pingId: Long,
        val uuid: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec

    object Codec : PePacketCodec() {
        override val id = 0x01
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is UnconnectedPingClientPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.pingId)
            stream.write(unconnectedBlabber)
            stream.writeLong(obj.uuid)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val pingId = stream.readLong()
            stream.skipBytes(unconnectedBlabber.size)
            return UnconnectedPingClientPePacket(
                    pingId = pingId,
                    uuid = stream.readLong()
            )
        }
    }
}

class ServerToClientHandshakePePacket(
        val serverKey: PublicKey,
        val token: ByteArray
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    
    object Codec : PePacketCodec() {
        override val id = 0x03
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ServerToClientHandshakePePacket) throw IllegalArgumentException()
            val base64Key = Base64.getEncoder().encode(obj.serverKey.encoded)
            stream.writeInt(base64Key.size)
            stream.write(base64Key)
            stream.write(obj.token)
        }
        val keyFactory = KeyFactory.getInstance("EC")
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ServerToClientHandshakePePacket(
                    serverKey = keyFactory.generatePublic(X509EncodedKeySpec(stream.readByteArray(stream.readInt()))),
                    token = stream.readByteArray(stream.readInt())
            )
        }
    }
}

class OpenConnectionRequest1PePacket(
        val protocolVersion: Byte,
        val mtuSize: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec

    object Codec : PePacketCodec() {
        override val id = 0x05
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is OpenConnectionRequest1PePacket) throw IllegalArgumentException()
            stream.write(unconnectedBlabber)
            stream.writeByte(obj.protocolVersion)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            stream.skipBytes(unconnectedBlabber.size)
            val protocolVersion = stream.readByte()
            return OpenConnectionRequest1PePacket(
                    protocolVersion = protocolVersion,
                    mtuSize = computeMtuSize(stream)
            )
        }
        fun computeMtuSize(stream: MinecraftInputStream) : Int {
            val packetIdSize = 1
            val protocolVersionSize = 1
            return packetIdSize + protocolVersionSize + unconnectedBlabber.size + stream.available()
        }
    }
}

class OpenConnectionRequest2PePacket(
        val remoteBindingAddress: SocketAddress,
        val mtuSize: Int,
        val clientUuid: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec

    object Codec : PePacketCodec() {
        override val id = 0x07
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is OpenConnectionRequest2PePacket) throw IllegalArgumentException()
            stream.write(unconnectedBlabber)
            stream.writeAddress(obj.remoteBindingAddress)
            stream.writeShort(obj.mtuSize)
            stream.writeLong(obj.clientUuid)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            stream.skipBytes(unconnectedBlabber.size)
            return OpenConnectionRequest2PePacket(
                    remoteBindingAddress = stream.readAddress(),
                    mtuSize = stream.readShort().toInt(),
                    clientUuid = stream.readLong()
            )
        }
    }
}

class ConnectionRequestPePacket(
        val uuid: Long,
        val timestamp: Long,
        val secured: Boolean
) : PePacket() {
    override val codec = Codec
    override val id = Codec.id

    object Codec : PePacketCodec() {
        override val id = 0x09
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ConnectionRequestPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.uuid)
            stream.writeLong(obj.timestamp)
            stream.writeBoolean(obj.secured)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ConnectionRequestPePacket(
                    uuid = stream.readLong(),
                    timestamp = stream.readLong(),
                    secured = stream.readBoolean()
            )
        }
    }
}

class UnconnectedPongServerPePacket(
        val pingId: Long,
        val serverId: Long,
        val serverListData: String
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec

    object Codec : PePacketCodec() {
        override val id = 0x1c
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is UnconnectedPongServerPePacket) throw IllegalArgumentException()
            stream.writeLong(obj.pingId)
            stream.writeLong(obj.serverId)
            stream.write(unconnectedBlabber)
            stream.writeUTF(obj.serverListData)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val pingId = stream.readLong()
            val serverId = stream.readLong()
            stream.skipBytes(unconnectedBlabber.size)
            return UnconnectedPongServerPePacket(
                    pingId = pingId,
                    serverId = serverId,
                    serverListData = stream.readUTF()
            )
        }
    }
}

class CompressionWrapperPePacket(
        val packets: List<PePacket>
) : PePacket() {
    constructor(vararg packets: PePacket) : this(packets.toList())

    fun serializedWithId(level: Int) : ByteArray {
        val bs = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(bs)
        mcStream.writeByte(id)
        Codec.serialize(this, mcStream, level)
        return bs.toByteArray()
    }
    
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x06
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is CompressionWrapperPePacket) throw IllegalArgumentException()
            serialize(obj, stream, Deflater.DEFAULT_COMPRESSION)
        }
        fun serialize(obj: CompressionWrapperPePacket, stream: MinecraftOutputStream, level: Int) {
            val bs = ByteArrayOutputStream()
            val mcStream = MinecraftOutputStream(bs)
            obj.packets.forEach {
                val serialized = it.serialized()
                mcStream.writeUnsignedVarInt(serialized.size + 1) // 1 for packet id
                mcStream.writeByte(it.id)
                mcStream.write(serialized)
            }
            val compressed = bs.toByteArray().compress(CompressionAlgorithm.ZLIB, level)
            stream.writeUnsignedVarInt(compressed.size)
            stream.write(compressed)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val compressedSize = stream.readUnsignedVarInt()
            if(compressedSize > 500_000) throw IllegalStateException("client tried to create a huge CompressionWrapper of $compressedSize bytes")
            val compressed = stream.readByteArray(compressedSize)
            
            val decompressed = compressed.decompress(CompressionAlgorithm.ZLIB)
            
            val codecMap = ServerBoundPeWrappedPackets.idToCodec

            val packets = mutableListOf<PePacket>()
            val decompressedStream = MinecraftInputStream(decompressed)
            while(decompressedStream.available() != 0) {
                val size = decompressedStream.readUnsignedVarInt()
                val bytes = decompressedStream.readByteArray(size)
                val packetStream = MinecraftInputStream(bytes)
                val id = packetStream.readByte()
                val codec = codecMap[id.toInt()] ?: throw IllegalStateException("Unknown pe message in compression wrapper ${id.toHexStr()}")
                val packet = codec.deserialize(packetStream)
                packets.add(packet)
            }
            return CompressionWrapperPePacket(
                    packets = *packets.toTypedArray()
            )
        }
    }
}

class OpenConnectionReply1PePacket(
        val serverId: Long,
        val secured: Boolean,
        val mtuSize: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x06
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is OpenConnectionReply1PePacket) throw IllegalArgumentException()
            stream.write(unconnectedBlabber)
            stream.writeLong(obj.serverId)
            stream.writeBoolean(obj.secured)
            stream.writeShort(obj.mtuSize)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            stream.skipBytes(unconnectedBlabber.size)
            return OpenConnectionReply1PePacket(
                    serverId = stream.readLong(),
                    secured = stream.readBoolean(),
                    mtuSize = stream.readShort().toInt()
            )
        }
    }
}

class OpenConnectionReply2PePacket(
        val serverId: Long,
        val clientEndpoint: SocketAddress,
        val mtuSize: Int,
        val secured: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x08
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is OpenConnectionReply2PePacket) throw IllegalArgumentException()
            stream.write(unconnectedBlabber)
            stream.writeLong(obj.serverId)
            stream.writeAddress(obj.clientEndpoint)
            stream.writeShort(obj.mtuSize)
            stream.writeBoolean(obj.secured)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            stream.skipBytes(unconnectedBlabber.size)
            return OpenConnectionReply2PePacket(
                    serverId = stream.readLong(),
                    clientEndpoint = stream.readAddress(),
                    mtuSize = stream.readShort().toInt(),
                    secured = stream.readBoolean()
            )
        }
    }
}

class SetTimePePacket(
        val time: Int,
        val started: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x0b
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetTimePePacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.time)
            stream.writeBoolean(obj.started)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetTimePePacket(
                    time = stream.readSignedVarInt(),
                    started = stream.readBoolean()
            )
        }
    }
}

enum class GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }
class StartGamePePacket(
        val entityId: Long,
        val runtimeEntityId: Long,
        val spawn: Vector3f,
        val yawAndPitch: Vector2f,
        val seed: Int,
        val dimension: Int,
        val generator: Int,
        val gamemode: GameMode,
        val difficulty: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val achievementsDisabled: Boolean,
        val dayCycleStopTime: Int,
        val eduEdition: Boolean,
        val rainLevel: Float,
        val lightningLevel: Float,
        val enableCommands: Boolean,
        val resourcePackRequired: Boolean,
        val levelId: String,
        val worldName: String
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x0c
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is StartGamePePacket) throw IllegalArgumentException()
            stream.writeSignedVarLong(obj.entityId)
            stream.writeUnsignedVarLong(obj.runtimeEntityId)
            stream.writeVector3fLe(obj.spawn)
            stream.writeVector2fLe(obj.yawAndPitch)
            stream.writeSignedVarInt(obj.seed)
            stream.writeSignedVarInt(obj.dimension)
            stream.writeSignedVarInt(obj.generator)
            stream.writeSignedVarInt(obj.gamemode.ordinal)
            stream.writeSignedVarInt(obj.difficulty)
            stream.writeSignedVarInt(obj.x)
            stream.writeSignedVarInt(obj.y)
            stream.writeSignedVarInt(obj.z)
            stream.writeBoolean(obj.achievementsDisabled)
            stream.writeSignedVarInt(obj.dayCycleStopTime)
            stream.writeBoolean(obj.eduEdition)
            stream.writeFloatLe(obj.rainLevel)
            stream.writeFloatLe(obj.lightningLevel)
            stream.writeBoolean(obj.enableCommands)
            stream.writeBoolean(obj.resourcePackRequired)
            stream.writeString(obj.levelId)
            stream.writeString(obj.worldName)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return StartGamePePacket(
                    entityId = stream.readSignedVarLong(),
                    runtimeEntityId = stream.readUnsignedVarLong(),
                    spawn = stream.readVector3fLe(),
                    yawAndPitch = stream.readVector2fLe(),
                    seed = stream.readSignedVarInt(),
                    dimension = stream.readSignedVarInt(),
                    generator = stream.readSignedVarInt(),
                    gamemode = GameMode.values()[stream.readSignedVarInt()],
                    difficulty = stream.readSignedVarInt(),
                    x = stream.readSignedVarInt(),
                    y = stream.readSignedVarInt(),
                    z = stream.readSignedVarInt(),
                    achievementsDisabled = stream.readBoolean(),
                    dayCycleStopTime = stream.readSignedVarInt(),
                    eduEdition = stream.readBoolean(),
                    rainLevel = stream.readFloat(),
                    lightningLevel = stream.readFloat(),
                    enableCommands = stream.readBoolean(),
                    resourcePackRequired = stream.readBoolean(),
                    levelId = stream.readString(),
                    worldName = stream.readString()
            )
        }
    }
}

class ConnectionRequestAcceptPePacket(
        val systemAddress: SocketAddress,
        val systemIndex: Int,
        val systemAddresses: Array<SocketAddress>,
        val incommingTimestamp: Long,
        val serverTimestamp: Long
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x10
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ConnectionRequestAcceptPePacket) throw IllegalArgumentException()
            stream.writeAddress(obj.systemAddress)
            stream.writeShort(obj.systemIndex)
            obj.systemAddresses.forEach { stream.writeAddress(it) }
            stream.writeLong(obj.incommingTimestamp)
            stream.writeLong(obj.serverTimestamp)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ConnectionRequestAcceptPePacket(
                    systemAddress = stream.readAddress(),
                    systemIndex = stream.readInt(),
                    systemAddresses = (1..10).map { stream.readAddress() }.toTypedArray(),
                    incommingTimestamp = stream.readLong(),
                    serverTimestamp = stream.readLong()
            )
        }
    }
}

enum class MoveMode { INTERPOLATE, TELEPORT, ROTATION }
class SetPlayerPositionPePacket(
        val entityId: Long,
        val x: Float,
        val y: Float,
        val z: Float,
        val headPitch: Float,
        val headYaw: Float,
        val bodyYaw: Float,
        val mode: MoveMode,
        val onGround: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x14
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetPlayerPositionPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeFloatLe(obj.x)
            stream.writeFloatLe(obj.y)
            stream.writeFloatLe(obj.z)
            stream.writeFloatLe(obj.headPitch)
            stream.writeFloatLe(obj.headYaw)
            stream.writeFloatLe(obj.bodyYaw)
            stream.writeByte(obj.mode.ordinal)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetPlayerPositionPePacket(
                    entityId = stream.readUnsignedVarLong(),
                    x = stream.readFloatLe(),
                    y = stream.readFloatLe(),
                    z = stream.readFloatLe(),
                    headPitch = stream.readFloatLe(),
                    headYaw = stream.readFloatLe(),
                    bodyYaw = stream.readFloatLe(),
                    mode = MoveMode.values()[stream.readByte().toInt()],
                    onGround = stream.readBoolean()
            )
        }
    }
}

enum class PlayerAttributeType(innerId: String, val minValue: Float, val maxValue: Float, val defaultValue: Float) {
    ABSORPTION("absorption", 0f, Float.MAX_VALUE, 0f),
    ATTACK_DAMAGE("attack_damage", 0f, Float.MAX_VALUE, 0f),
    KNOCKBACK_RESISTANCE("knockback_resistance", 0f, 1f, 0f),
    LUCK("luck", -1025f, 1024f, 0f),
    FALL_DAMAGE("fall_damage", 0f, Float.MAX_VALUE, 1f),
    HEALTH("health", 0f, 20f, 20f),
    MOVEMENT_SPEED("movement", 0f, Float.MAX_VALUE, 0.1f),
    FOLLOW_RANGE("follow_range", 0f, 2048f, 16f),
    SATURATION("player.saturation", 0f, 20f, 5f),
    EXHAUSTION("player.exhaustion", 0f, 5f, 0.41f),
    HUNGER("player.hunger", 0f, 20f, 20f),
    EXPERIENCE_LEVEL("player.level", 0f, 24791f, 0f),
    EXPERIENCE("player.experience", 0f, 1f, 0f),
    ;
    
    val id = "minecraft:$innerId"
    
    fun value(value: Float) : PlayerAttribute {
        return PlayerAttribute(minValue, maxValue, value, defaultValue, id)
    }
    
    companion object {
        val byId = values().associateBy { it.id }
    }
}
class PlayerAttribute(
        val minValue: Float,
        val maxValue: Float,
        val value: Float,
        val defaultValue: Float,
        val name: String
) {
    object Codec : PeCodec<PlayerAttribute> {
        override fun serialize(obj: PlayerAttribute, stream: MinecraftOutputStream) {
            stream.writeFloatLe(obj.minValue)
            stream.writeFloatLe(obj.maxValue)
            stream.writeFloatLe(obj.value)
            stream.writeFloatLe(obj.defaultValue)
            stream.writeString(obj.name)
        }
        override fun deserialize(stream: MinecraftInputStream): PlayerAttribute {
            return PlayerAttribute(
                    minValue = stream.readFloat(),
                    maxValue = stream.readFloat(),
                    value = stream.readFloat(),
                    defaultValue = stream.readFloat(),
                    name = stream.readString()
            )
        }
    }
    
    companion object {
        val defaults = PlayerAttributeType.values().map { it.value(it.defaultValue) }
    }
}
class SetAttributesPePacket(
        val entityId: Long,
        val attributes: List<PlayerAttribute>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x1f
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetAttributesPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeUnsignedVarInt(obj.attributes.size)
            obj.attributes.forEach { PlayerAttribute.Codec.serialize(it, stream) }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetAttributesPePacket(
                    entityId = stream.readUnsignedVarLong(),
                    attributes = (1..stream.readUnsignedVarInt()).map { PlayerAttribute.Codec.deserialize(stream) }
            )
        }
    }
}

enum class PlayerAction(val id: Int) {
    START_BREAK(0),
    ABORT_BREAK(1),
    STOP_BREAK(2),
    UNKNOWN_3(3),
    UNKNOWN_4(4),
    RELEASE_ITEM(5),
    STOP_SLEEPING(6),
    RESPAWN(7),
    JUMP(8),
    START_SPRINT(9),
    STOP_SPRINT(10),
    START_SNEAK(11),
    STOP_SNEAK(12),
    DIMENSION_CHANGE(13),
    ABORT_DIMENSION_CHANGE(14),
    START_GLIDE(15),
    STOP_GLIDE(16)
}
class PlayerActionPePacket(
        val entityId: Long,
        val action: PlayerAction,
        val x: Int,
        val y: Int,
        val z: Int,
        val blockFace: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x24
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerActionPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeSignedVarInt(obj.action.ordinal)
            stream.writeSignedVarInt(obj.x)
            stream.writeUnsignedVarInt(obj.y)
            stream.writeSignedVarInt(obj.z)
            stream.writeSignedVarInt(obj.blockFace)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return PlayerActionPePacket(
                    entityId = stream.readUnsignedVarLong(),
                    action = PlayerAction.values()[stream.readSignedVarInt()],
                    x = stream.readSignedVarInt(),
                    y = stream.readUnsignedVarInt(),
                    z = stream.readSignedVarInt(),
                    blockFace = stream.readSignedVarInt()
            )
        }
    }
}

class SetAdventureSettingsPePacket(
        val settings: AdventureSettingsFlags,
        val permissionLevel: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x37
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetAdventureSettingsPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.settings.bitField)
            stream.writeUnsignedVarInt(obj.permissionLevel)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetAdventureSettingsPePacket(
                    settings = AdventureSettingsFlags(stream.readUnsignedVarInt()),
                    permissionLevel = stream.readUnsignedVarInt()
            )
        }
    }
}

class FullChunkDataPePacket(
        val x: Int,
        val z: Int,
        val data: ByteArray
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x3a
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is FullChunkDataPePacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.x)
            stream.writeSignedVarInt(obj.z)
            stream.writeUnsignedVarInt(obj.data.size)
            stream.write(obj.data)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return FullChunkDataPePacket(
                    x = stream.readSignedVarInt(),
                    z = stream.readSignedVarInt(),
                    data = stream.readByteArray(stream.readUnsignedVarInt())
            )
        }
    }
}

class SetCommandsEnabledPePacket(
        val enabled: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x3b
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetCommandsEnabledPePacket) throw IllegalArgumentException()
            stream.writeBoolean(obj.enabled)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetCommandsEnabledPePacket(
                    enabled = stream.readBoolean()
            )
        }
    }
}

class SetDifficultyPePacket(
        val difficulty: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x3c
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetDifficultyPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.difficulty)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetDifficultyPePacket(
                    difficulty = stream.readUnsignedVarInt()
            )
        }
    }
}

class ChunkRadiusRequestPePacket(
        val desiredChunkRadius: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x44
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ChunkRadiusRequestPePacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(id)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ChunkRadiusRequestPePacket(
                    desiredChunkRadius = stream.readSignedVarInt()
            )
        }
    }
}

class SetChunkRadiusPePacket(
        val chunkRadius: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x45
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetChunkRadiusPePacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.chunkRadius)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetChunkRadiusPePacket(
                    chunkRadius = stream.readSignedVarInt()
            )
        }
    }
}

class EncryptionWrapperPePacket(
        val payload: ByteArray
) : PePacket() {
    constructor(payload: PePacket) : this(payload.serializedWithId())
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0xFE
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is EncryptionWrapperPePacket) throw IllegalArgumentException()
            stream.write(obj.payload)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return EncryptionWrapperPePacket(
                    payload = stream.readRemainingBytes()
            )
        }
    }
}