package world.crafty.pc.proto

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.parsetools.RecordParser

class LengthPrefixedHandler(consumer: (Buffer) -> Unit) : Handler<Buffer> {
    lateinit var parser : RecordParser
    var expectedToken = FrameToken.SIZE
    var sizePrefix: TransientVarInt

    init {
        sizePrefix = TransientVarInt()
        parser = RecordParser.newFixed(1) {
            when(expectedToken) {
                FrameToken.SIZE -> {
                    val byte = it.getByte(0)
                    sizePrefix.write(byte)
                    if(! sizePrefix.wantsMore) {
                        expectedToken = FrameToken.BODY
                        parser.fixedSizeMode(sizePrefix.value)
                    }
                }
                FrameToken.BODY -> {
                    consumer(it)
                    sizePrefix = TransientVarInt()
                    expectedToken = FrameToken.SIZE
                    parser.fixedSizeMode(1)
                }
            }
        }
    }

    override fun handle(event: Buffer) {
        return parser.handle(event)
    }

    enum class FrameToken { SIZE, BODY }

    class TransientVarInt {
        private var lastReceive = 0
        var value = 0
            get() = field or ((lastReceive and 0x7F) shl (size * 7))
            private set
        var size = 0
            private set

        val wantsMore: Boolean
            get() = (lastReceive and 0x80) == 0x80

        fun write(byte: Byte) {
            lastReceive = byte.toInt()
            if(! wantsMore) return
            value = value
            ++size
        }
    }
}
