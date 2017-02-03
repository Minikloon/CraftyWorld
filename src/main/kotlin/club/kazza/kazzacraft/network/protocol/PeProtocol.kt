package club.kazza.kazzacraft.network.protocol

import club.kazza.kazzacraft.network.protocol.jwt.PeJwt
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import club.kazza.kazzacraft.utils.CompressionAlgorithm
import club.kazza.kazzacraft.utils.compress
import club.kazza.kazzacraft.utils.decompress
import io.vertx.core.json.JsonObject
import io.vertx.core.net.SocketAddress
import java.io.InputStream
import java.io.OutputStream
import java.util.*

abstract class InboundPePacketList {
    protected abstract fun getCodecs() : List<PePacket.PePacketCodec>
    val idToCodec: Map<Int, PePacket.PePacketCodec>
    init {
        val mappedPackets = mutableMapOf<Int, PePacket.PePacketCodec>()
        for(codec in getCodecs())
            mappedPackets[codec.id] = codec
        idToCodec = mappedPackets
    }
}

object InboundPeRaknetPackets : InboundPePacketList() {
    override fun getCodecs(): List<PePacket.PePacketCodec> {
        return listOf(
                UnconnectedPingClientPePacket.Codec,
                OpenConnectionRequest1PePacket.Codec,
                OpenConnectionRequest2PePacket.Codec
        )
    }
}

object InboundPePackets : InboundPePacketList() {
    override fun getCodecs(): List<PePacket.PePacketCodec> {
        return listOf(
                ConnectionRequestPePacket.Codec,
                NewIncomingConnection.Codec,
                ConnectedPingPePacket.Codec,
                EncryptionWrapperPePacket.Codec,
                LoginPePacket.Codec
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
            
            val payloadSize = stream.readVarInt()
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

class ConnectionRequestAcceptPePacket(
        val systemAddress: SocketAddress,
        val systemIndex: Int,
        val systemAddresses: Array<SocketAddress>,
        val incommingTimestamp: Long,
        val serverTimestamp: Long
) : PePacket() {
    override val codec = Codec
    override val id = Codec.id

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

class EncryptionWrapperPePacket(
        val payload: ByteArray
) : PePacket() {
    override val codec = Codec
    override val id = Codec.id
    
    object Codec : PePacketCodec() {
        override val id = 0xFE
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is EncryptionWrapperPePacket) throw IllegalArgumentException()
            stream.write(obj.payload)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val bytes = ByteArray(stream.available())
            stream.read(bytes)
            return EncryptionWrapperPePacket(bytes)
        }
    }
}