package world.crafty.common.serialization

import java.io.DataOutput
import java.io.DataOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream

class LittleEndianDataOutputStream(out: OutputStream) : FilterOutputStream(DataOutputStream(out)), DataOutput {
    private val dataOut = out as DataOutputStream
    
    override fun write(b: ByteArray?, off: Int, len: Int) {
        dataOut.write(b, off, len)
    }

    override fun writeBoolean(v: Boolean) {
        dataOut.writeBoolean(v)
    }

    override fun writeByte(v: Int) {
        dataOut.writeByte(v)
    }

    override fun writeBytes(s: String?) {
        dataOut.writeBytes(s)
    }

    override fun writeChar(v: Int) {
        writeShort(v)
    }

    override fun writeChars(s: String) {
        for(i in 0 until s.length) {
            writeChar(s[i].toInt())
        }
    }

    override fun writeDouble(v: Double) {
        writeLong(java.lang.Double.doubleToLongBits(v))
    }

    override fun writeFloat(v: Float) {
        writeInt(java.lang.Float.floatToIntBits(v))
    }

    override fun writeInt(v: Int) {
        repeat(4) {
            val shift = 8*it
            out.write((v shr shift) and 0xFF)
        }
    }

    override fun writeLong(v: Long) {
        val mask = 0xFF.toLong()
        repeat(8) {
            val shift = 8*it
            out.write(((v shr shift) and mask).toInt())
        }
    }

    override fun writeShort(v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
    }

    override fun writeUTF(s: String?) {
        dataOut.writeUTF(s)
    }

    override fun close() {
        out.close()
    }
}