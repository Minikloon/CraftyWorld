package club.kazza.kazzacraft.network.serialization

import club.kazza.kazzacraft.Location
import club.kazza.kazzacraft.network.protocol.PcPacket
import club.kazza.kazzacraft.world.ChunkSection
import io.vertx.core.json.Json
import io.vertx.core.net.SocketAddress
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.*

class MinecraftOutputStream(stream: OutputStream) : DataOutputStream(stream) {
    constructor() : this(ByteArrayOutputStream())

    fun writeUnsignedVarInt(value: Int) {
        var v = value
        val bytes = ArrayList<Byte>(4)
        while((v and -0x80) != 0x00) {
            bytes.add((v and 0x7F or 0x80).toByte())
            v = (v shr 7)
        }
        bytes.add(v.toByte())
        write(bytes.toByteArray())
    }
    
    fun writeSignedVarInt(value: Int) {
        writeUnsignedVarInt((value shl 1) xor (value shr 31))
    }
    
    fun writeUnsignedVarLong(value: Long) {
        var v = value
        val bytes = ArrayList<Byte>(4)
        while((v and -0x80L) != 0x00L) {
            bytes.add((v and 0x7FL or 0x80L).toByte())
            v = (v shr 7)
        }
        bytes.add(v.toByte())
        write(bytes.toByteArray())
    }
    
    fun writeSignedVarLong(value: Long) {
        writeUnsignedVarLong((value shl 1) xor (value shr 63))
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

    fun writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeUnsignedVarInt(bytes.size)
        write(bytes)
    }

    fun writeJson(value: Any) {
        val json = Json.encode(value)
        writeString(json)
    }

    fun writeBlockLocation(value: Location) {
        val x = value.x.toLong()
        val y = value.y.toLong()
        val z = value.z.toLong()
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
    
    fun writeVector2fLe(vector: Vector2f) {
        writeFloatLe(vector.x)
        writeFloatLe(vector.y)
    }
    
    fun writeVector3fLe(vector: Vector3f) {
        writeFloatLe(vector.x)
        writeFloatLe(vector.y)
        writeFloatLe(vector.z)
    }

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