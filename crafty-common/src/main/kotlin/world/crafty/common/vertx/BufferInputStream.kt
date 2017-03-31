package world.crafty.common.vertx

import io.vertx.core.buffer.Buffer
import java.io.InputStream

class BufferInputStream(val buffer: Buffer, private var pos: Int = 0) : InputStream() {
    override fun read(): Int {
        val byte = buffer.getUnsignedByte(pos).toInt()
        ++pos
        return byte
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        if (b == null)
            throw NullPointerException()
        if (off < 0 || len < 0 || len > b.size - off)
            throw IndexOutOfBoundsException()
        if(pos + len > buffer.length())
            return -1
        if (len == 0)
            return 0
        
        buffer.getBytes(pos, pos + len, b, off)
        pos += len
        return len
    }

    override fun skip(n: Long): Long {
        val skipped = Math.min(n.toInt(), buffer.length() - pos)
        pos += skipped
        return skipped.toLong()
    }
    
    override fun available(): Int {
        return buffer.length() - pos
    }
}