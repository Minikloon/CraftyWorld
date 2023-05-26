package world.crafty.common.serialization

import io.vertx.core.json.Json
import io.vertx.core.net.SocketAddress
import io.vertx.core.net.impl.SocketAddressImpl
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector3ic
import world.crafty.common.Angle256
import world.crafty.common.Location
import world.crafty.common.utils.toHexStr
import world.crafty.common.vertx.BufferInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.util.*
import kotlin.reflect.KClass

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
    
    fun readZigzagVarInt() : Int {
        val varInt = readUnsignedVarInt()
        return (varInt ushr 1) xor (varInt shl 31)
    }
    
    fun readSignedVarInt() : Int {
        var value = 0
        var bytes = 0
        do {
            val b = readByte().toInt()
            value = value or (b and 127 shl bytes++ * 7)
        } while (b and 128 == 128)
        return value
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
    
    fun readZigzagVarLong() : Long {
        val varLong = readUnsignedVarLong()
        return (varLong shr 1) xor -(varLong and 1)
    }
    
    fun readSignedVarLong() : Long {
        var value = 0L
        var bytes = 0
        do {
            val b = readByte().toLong()
            value = value or ((b and 127L) shl (bytes++ * 7))
        } while (b and 128 == 128L)
        return value
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

    fun readIntArray(count: Int) : IntArray {
        val entries = IntArray(count)
        for(i in 0 until count) {
            entries[i] = readInt()
        }
        return entries
    }
    
    fun readRemainingBytes() : ByteArray {
        if(`in` !is ByteArrayInputStream && `in` !is BufferInputStream) 
            throw NotImplementedError("readRemainingBytes only available for ByteArrayInputStream or BufferInputStream")
        return readByteArray(available())
    }

    fun readSignedString(length: Int = -1) : String {
        val size = if(length == -1) readSignedVarInt() else length
        val bytes = ByteArray(size)
        read(bytes)
        return String(bytes)
    }

    fun readUnsignedString(length: Int = -1) : String {
        val size = if(length == -1) readUnsignedVarInt() else length
        val bytes = ByteArray(size)
        read(bytes)
        return String(bytes)
    }

    fun <T: Any> readJson(clazz: KClass<T>) : T {
        val json = readSignedString()
        return Json.decodeValue(json, clazz.java)
    }

    fun readBlockLocation() : Vector3ic {
        val value = readLong()
        val x = value shr 38
        val y = value shr 0xfff
        val z = value shl 38 shr 38
        return Vector3i(x.toInt(), y.toInt(), z.toInt())
    }

    fun readUuid() : UUID {
        return UUID(readLong(), readLong())
    }

    fun readAddress() : SocketAddress {
        val version = readByte().toInt()
        return when(version) {
            4 -> {
                val host = "${readByte()}.${readByte()}.${readByte()}.${readByte()}"
                val port = readShort().toInt()
                SocketAddressImpl(port, host)
            }
            6 ->  {
                val family = readShort()
                val port = readShort()
                val flow = readLong()
                val address = readByteArray(16)
                val addressStr = ShortArray(8) {
                    ((address[it].toInt() shl 8) or (address[it].toInt())).toShort()
                }.map { it.toHexStr().substring(2) }.joinToString(":")
                SocketAddressImpl(port.toInt(), addressStr)
            }
            else -> throw IllegalStateException("Unknown IP version $version")
        }
    }
    
    fun readVector2f() : Vector2f {
        return Vector2f(readFloat(), readFloat())
    }
    
    fun readVector3f() : Vector3f {
        return Vector3f(readFloat(), readFloat(), readFloat())
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
    
    fun readAngle() : Angle256 {
        return Angle256(readByte())
    }
    
    fun readLocation() : Location {
        return Location(readFloat(), readFloat(), readFloat(), readAngle(), readAngle())
    }
}