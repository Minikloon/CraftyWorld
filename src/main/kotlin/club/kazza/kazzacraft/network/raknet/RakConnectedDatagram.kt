package club.kazza.kazzacraft.network.raknet

import club.kazza.kazzacraft.network.protocol.PeCodec
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream
import io.vertx.core.buffer.Buffer

class RakConnectedDatagram(val headerFlags: RakDatagramFlags, val sequenceNumber: Int, val data: Buffer) {    
    val dataAsStream : MinecraftInputStream
        get() = MinecraftInputStream(data.bytes)
    
    object Codec : PeCodec<RakConnectedDatagram> {
        override fun serialize(obj: RakConnectedDatagram, stream: MinecraftOutputStream) {
            stream.writeByte(obj.headerFlags.packetId)
            stream.write3BytesInt(obj.sequenceNumber)
            stream.write(obj.data.bytes)
        }

        override fun deserialize(stream: MinecraftInputStream): RakConnectedDatagram {
            return RakConnectedDatagram(
                    headerFlags = RakDatagramFlags(stream.readByte()),
                    sequenceNumber = stream.read3BytesInt(),
                    data = Buffer.buffer(stream.readByteArray(stream.available()))
            )
        }
    }
}