package world.crafty.common.vertx

import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder
import io.vertx.core.buffer.Buffer

private val factory = DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE)

class MultipartRequestElements(
        val template: HttpRequest,
        val buffer: Buffer
)

fun encodeMultipart(encoderFiller: (HttpPostRequestEncoder) -> Unit) : MultipartRequestElements {
    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/")
    val encoder = HttpPostRequestEncoder(factory, request, true)
    encoderFiller(encoder)
    encoder.finalizeRequest()

    val buffer: Buffer
    if (encoder.isChunked) {
        buffer = Buffer.buffer()
        while (true) {
            val chunk = encoder.readChunk(UnpooledByteBufAllocator(false))
            val content = chunk.content()
            if (content.readableBytes() == 0) {
                break
            }
            buffer.appendBuffer(Buffer.buffer(content))
        }
    } else {
        val content = request.content()
        buffer = Buffer.buffer(content)
    }
    
    return MultipartRequestElements(request, buffer)
}

