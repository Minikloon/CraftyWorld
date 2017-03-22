package world.crafty.common.serialization

import io.vertx.core.json.Json
import io.vertx.core.net.SocketAddress
import org.joml.*
import world.crafty.common.Angle256
import world.crafty.common.Location
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.*

class MinecraftOutputStream(stream: OutputStream) : DataOutputStream(stream) {
    fun writeUnsignedVarInt(value: Int) {
        var v = value
        while((v and -0x80) != 0x00) {
            writeByte((v and 0x7F or 0x80).toByte())
            v = (v shr 7)
        }
        writeByte(v)
    }

    fun writeZigzagVarInt(value: Int) { // zigzag means signed
        writeUnsignedVarInt((value shl 1) xor (value shr 31))
    }
    
    fun writeSignedVarInt(value: Int) {
        var v = value
        do {
            var byte = (v and 0b01111111)
            v = (v ushr 7)
            if(v != 0)
                byte = byte or 0b10000000
            writeByte(byte)
        } while(v != 0)
    }
    
    fun writeUnsignedVarLong(value: Long) {
        var v = value
        while((v and -0x80L) != 0x00L) {
            writeByte((v and 0x7FL or 0x80L).toByte())
            v = (v shr 7)
        }
        writeByte(v.toByte())
    }

    fun writeZigzagVarLong(value: Long) {
        writeUnsignedVarLong((value shl 1) xor (value shr 63))
    }
    
    fun writeSignedVarLong(value: Long) {
        var v = value
        do {
            var byte = (v and 0b01111111).toInt()
            v = (v ushr 7)
            if(v != 0L)
                byte = byte or 0b10000000
            writeByte(byte)
        } while(v != 0L)
    }

    fun writeIntLe(value: Int) {
        write((value shr 0) and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }
    
    fun write3BytesInt(value: Int) {
        writeByte(value.toByte())
        writeByte((value ushr 8).toByte())
        writeByte((value ushr 16).toByte())
    }

    fun writeByte(value: Byte) {
        writeByte(value.toInt())
    }

    fun writeSignedString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeSignedVarInt(bytes.size)
        write(bytes)
    }
    
    fun writeUnsignedString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeUnsignedVarInt(bytes.size)
        write(bytes)
    }

    fun writeJson(value: Any) {
        val json = Json.encode(value)
        writeSignedString(json)
    }

    fun writeBlockLocation(value: Vector3ic) {
        val x = value.x().toLong()
        val y = value.y().toLong()
        val z = value.z().toLong()
        writeLong((x and 0x3ffffff) shl 38 or (y and 0xfff) shl 26 or z and 0x3ffffff)
    }

    fun writeUuid(value: UUID) {
        writeLong(value.mostSignificantBits)
        writeLong(value.leastSignificantBits)
    }

    fun writeAddress(address: SocketAddress) {
        writeByte(4)
        address.host()
                .split(".")
                .map(String::toInt)
                .forEach { writeByte(it) }
        writeShort(address.port())
    }
    
    fun writeFloatLe(value: Float) {
        writeIntLe(java.lang.Float.floatToIntBits(value))
    }
    
    fun writeVector2fLe(vector: Vector2fc) {
        writeFloatLe(vector.x())
        writeFloatLe(vector.y())
    }
    
    fun writeVector3fLe(vector: Vector3fc) {
        writeFloatLe(vector.x())
        writeFloatLe(vector.y())
        writeFloatLe(vector.z())
    }
    
    fun writeAngle(angle: Angle256) {
        writeByte(angle.increment)
    }
    
    fun writeLocation(loc: Location) {
        writeFloat(loc.x)
        writeFloat(loc.y)
        writeFloat(loc.z)
        writeAngle(loc.pitch)
        writeAngle(loc.yaw)
    }

    /*
    fun writePacket(packet: PcPacket, compressed: Boolean = false) {
        val contentStream = ByteArrayOutputStream()
        val contentMcStream = MinecraftOutputStream(contentStream)
        packet.serialize(contentMcStream)
        val contentBytes = contentStream.toByteArray()

        writeUnsignedVarInt(contentBytes.size + 1 + (if(compressed) 1 else 0))
        if(compressed) writeUnsignedVarInt(0)
        writeUnsignedVarInt(packet.id)
        write(contentBytes)
    }
    */

    companion object {        
        fun varIntSize(value: Int) : Int {
            var v = value
            var size = 0
            while((v and -0x80) != 0x00) {
                ++size
                v = (v shr 7)
            }
            return size + 1
        }
    }
}