package club.kazza.kazzacraft.network.raknet

import club.kazza.kazzacraft.network.protocol.PeCodec
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import io.vertx.core.buffer.Buffer
import java.io.ByteArrayOutputStream

class RakDatagram(val headerFlags: RakDatagramFlags, val sequenceNumber: Int, val data: ByteArray) {    
    val dataAsStream : MinecraftInputStream
        get() = MinecraftInputStream(data)
    
    fun serialized() : ByteArray {
        val bs = ByteArrayOutputStream(data.size + headerSize)
        val mcStream = MinecraftOutputStream(bs)
        Codec.serialize(this, mcStream)
        return bs.toByteArray()
    }
    
    object Codec : PeCodec<RakDatagram> {
        override fun serialize(obj: RakDatagram, stream: MinecraftOutputStream) {
            stream.writeByte(obj.headerFlags.packetId)
            stream.write3BytesInt(obj.sequenceNumber)
            stream.write(obj.data)
        }

        override fun deserialize(stream: MinecraftInputStream): RakDatagram {
            return RakDatagram(
                    headerFlags = RakDatagramFlags(stream.readByte()),
                    sequenceNumber = stream.read3BytesInt(),
                    data = stream.readRemainingBytes()
            )
        }
    }
    
    companion object {
        val headerSize = 4
    }
}