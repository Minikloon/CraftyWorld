package world.crafty.pe.raknet

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PeCodec
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