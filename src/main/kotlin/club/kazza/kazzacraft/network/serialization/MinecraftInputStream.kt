package club.kazza.kazzacraft.network.serialization

import club.kazza.kazzacraft.Location
import io.vertx.core.json.Json
import io.vertx.core.net.SocketAddress
import io.vertx.core.net.impl.SocketAddressImpl
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.util.*

class MinecraftInputStream(stream : InputStream) : DataInputStream(stream) {
    constructor(bytes: ByteArray) : this(ByteArrayInputStream(bytes))
    constructor(bytes: ByteArray, offset: Int, length: Int) : this(ByteArrayInputStream(bytes, offset, length))

    fun readUnsignedVarInt() : Int {
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
    
    fun readSignedVarInt() : Int {
        val varInt = readUnsignedVarInt()
        return (varInt ushr 1) xor (varInt shl 31)
    }
    
    fun readUnsignedVarLong() : Long {
        var value = 0L
        var size = 0

        var b: Long
        while(true) {
            b = readUnsignedByte().toLong()
            if(b and 0x80L != 0x80L) break
            value = value or ((b and 0x7FL) shl (size++ * 7))
        }
        value = value or ((b and 0x7F) shl (size * 7))
        return value
    }
    
    fun readSignedVarLong() : Long {
        val varLong = readUnsignedVarLong()
        return (varLong shr 1) xor -(varLong and 1)
    }
    
    fun readIntLe() : Int {
        val b1 = read()
        val b2 = read()
        val b3 = read()
        val b4 = read()
        return (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or (b1 shl 0)
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

    fun readString(length: Int = -1) : String {
        val size = if(length == -1) readUnsignedVarInt() else length
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
    
    fun readFloatLe() : Float {
        return java.lang.Float.intBitsToFloat(readIntLe())
    }
    
    fun readVector2fLe() : Vector2f {
        return Vector2f(readFloatLe(), readFloatLe())
    }
    
    fun readVector3fLe() : Vector3f {
        return Vector3f(readFloatLe(), readFloatLe(), readFloatLe())
    }
}