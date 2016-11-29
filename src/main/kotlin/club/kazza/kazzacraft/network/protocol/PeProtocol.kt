package club.kazza.kazzacraft.network.protocol

import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
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
        return listOf()
    }
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

class AckPePacket(
        val ranges: List<IntRange>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0xc0
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AckPePacket) throw IllegalArgumentException()
            obj.ranges.forEach {
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
                    ranges[it] = datagramSeqNo..datagramSeqNo
                }
                else {
                    val first = stream.read3BytesInt()
                    val last = stream.read3BytesInt()
                    ranges[it] = first..last
                }
            }
            return AckPePacket(ranges)
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

class UnconnectedPongServerPePacket(
        val pingId: Long,
        val serverId: Long,
        val serverName: String
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
            stream.writeUTF(obj.serverName)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val pingId = stream.readLong()
            val serverId = stream.readLong()
            stream.skipBytes(unconnectedBlabber.size)
            return UnconnectedPongServerPePacket(
                    pingId = pingId,
                    serverId = serverId,
                    serverName = stream.readUTF()
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