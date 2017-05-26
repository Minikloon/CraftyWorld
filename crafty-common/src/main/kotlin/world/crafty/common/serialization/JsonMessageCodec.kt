package world.crafty.common.serialization

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import kotlin.reflect.KClass

class JsonMessageCodec<T: Any>(private val clazz: KClass<T>) : MessageCodec<T, T> {
    override fun encodeToWire(buffer: Buffer, s: T) {
        val json = Json.encode(s)
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        buffer.appendInt(jsonBytes.size)
        buffer.appendBytes(jsonBytes)
    }

    override fun transform(s: T): T {
        return s
    }

    override fun systemCodecID(): Byte {
        return -1
    }

    override fun name(): String {
        return "json:${clazz.qualifiedName}"
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): T {
        val size = buffer.getInt(pos)
        val jsonBytes = ByteArray(size)
        buffer.getBytes(pos + 4, pos + 4 + size, jsonBytes)
        val json = jsonBytes.toString(Charsets.UTF_8)
        return JsonObject(json).mapTo(clazz.java)
    }
}