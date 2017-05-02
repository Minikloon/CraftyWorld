package world.crafty.pe.proto.packets.mixed

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.compressed
import world.crafty.common.utils.decompressed
import world.crafty.common.utils.toHexStr
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.ServerBoundPeWrappedPackets
import java.util.zip.Deflater

class CompressionWrapperPePacket(
        val packets: List<PePacket>
) : PePacket() {
    constructor(vararg packets: PePacket) : this(packets.toList())

    override val expectedSize = packets.sumBy { it.expectedSize }
    
    fun serializedWithId(level: Int) : ByteArray {
        return MinecraftOutputStream.serialized(expectedSize) { stream ->
            stream.writeByte(id)
            Codec.serialize(this, stream, level)
        }
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
            val wrappedPackets = MinecraftOutputStream.serialized { mcStream ->
                obj.packets.forEach {
                    val serialized = it.serialized()
                    mcStream.writeUnsignedVarInt(serialized.size + 1) // 1 for packet id
                    mcStream.writeByte(it.id)
                    mcStream.write(serialized)
                }
            }
            val compressed = wrappedPackets.compressed(CompressionAlgorithm.ZLIB, level)
            stream.writeUnsignedVarInt(compressed.size)
            stream.write(compressed)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val compressedSize = stream.readUnsignedVarInt()
            if(compressedSize > 500_000) throw IllegalStateException("client tried to create a huge CompressionWrapper of $compressedSize bytes")
            val compressed = stream.readByteArray(compressedSize)

            val decompressed = compressed.decompressed(CompressionAlgorithm.ZLIB)

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