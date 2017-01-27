package club.kazza.kazzacraft.network.serialization

import club.kazza.kazzacraft.Location
import io.vertx.core.json.Json
import io.vertx.core.net.SocketAddress
import io.vertx.core.net.impl.SocketAddressImpl
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.util.*

class MinecraftInputStream(stream : InputStream) : DataInputStream(stream) {
    constructor(bytes: ByteArray) : this(ByteArrayInputStream(bytes))
    constructor(bytes: ByteArray, offset: Int, length: Int) : this(ByteArrayInputStream(bytes, offset, length))

    fun readVarInt() : Int {
        var value = 0
        var size = 0

        var b: Int
        while(true) {
            b = readUnsignedByte()
            if(b and 0x80 != 0x80) break
            value = value or ((b and 0x7F) shl (size++ * 7))
        }
        value = value or ((b and 0x7F) shl (size * 7))
        return value
    }

    fun read3BytesInt() : Int {
        val small = readUnsignedByte()
        val middle = readUnsignedByte()
        val big = readUnsignedByte()
        return (big shl 16) or (middle shl 8) or small
    }
    
    fun readByteArray(count: Int) : ByteArray {
        val bytes = ByteArray(count)
        read(bytes)
        return bytes
    }
    
    fun readRemainingBytes() : ByteArray {
        if(`in` !is ByteArrayInputStream) 
            throw NotImplementedError("readRemainingBytes only available for ByteArrayInputStream")
        return readByteArray(available())
    }

    fun readString() : String {
        val size = readVarInt()
        val bytes = ByteArray(size)
        read(bytes)
        return String(bytes)
    }

    fun <T> readJson(clazz: Class<T>) : T {
        val json = readString()
        return Json.decodeValue(json, clazz)
    }

    fun readBlockLocation() : Location {
        val value = readLong()
        val x = value shr 38
        val y = value shr 0xfff
        val z = value shl 38 shr 38
        return Location(x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun readUuid() : UUID {
        return UUID(readLong(), readLong())
    }

    fun readAddress() : SocketAddress {
        val version = readByte().toInt()
        val host = "${readByte()}.${readByte()}.${readByte()}.${readByte()}"
        val port = readShort().toInt()
        return SocketAddressImpl(port, host)
    }
}