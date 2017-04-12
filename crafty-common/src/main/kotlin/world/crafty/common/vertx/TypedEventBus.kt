package world.crafty.common.vertx

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import kotlin.reflect.KClass

/* TODO: Use this, can't atm because of a bug in Kotlin <= 1.1.1
inline fun <reified T> EventBus.typedConsumer(prefix: String = "", crossinline handler: (Message<T>) -> Unit) {
    val address = "$prefix:${T::class.java.simpleName}"
    consumer<T>(address) {
        handler(it)
    }
}
*/

@Deprecated("Use reified version")
fun <T: Any> EventBus.typedConsumer(prefix: String = "", clazz: KClass<T>, handler: (Message<T>) -> Unit) : MessageConsumer<T> {
    val address = "$prefix:${clazz.java.simpleName}"
    return consumer<T>(address) { handler(it) }
}

fun EventBus.typedPublish(prefix: String = "", obj: Any) {
    val address = "$prefix:${obj::class.java.simpleName}"
    publish(address, obj)
}

fun EventBus.typedSend(prefix: String = "", obj: Any) {
    val address = "$prefix:${obj::class.java.simpleName}"
    send(address, obj)
}

fun <TReply> EventBus.typedSend(prefix: String = "", obj: Any, replyHandler: Handler<AsyncResult<Message<TReply>>>) {
    val address = "$prefix:${obj::class.java.simpleName}"
    send<TReply>(address, obj, replyHandler)
}

inline fun <TReply> EventBus.typedSend(prefix: String = "", obj: Any, crossinline replyHandler: (AsyncResult<Message<TReply>>) -> Unit) {
    val address = "$prefix:${obj::class.java.simpleName}"
    send<TReply>(address, obj) { replyHandler(it) }
}