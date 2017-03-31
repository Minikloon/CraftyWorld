package world.crafty.common.vertx

import io.vertx.core.AsyncResult
import io.vertx.core.Context
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.impl.VertxImpl
import io.vertx.ext.web.client.HttpResponse
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.Delay
import kotlinx.coroutines.experimental.cancelFutureOnCompletion
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.*

class VertxContinuation<in T>(val vertxContext: Context, val cont: Continuation<T>) : Continuation<T> by cont {
    override fun resume(value: T) {
        vertxContext.runOnContext { cont.resume(value) }
    }

    override fun resumeWithException(exception: Throwable) {
        vertxContext.runOnContext { cont.resumeWithException(exception) }
    }
}

object CurrentVertx : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor, Delay {
    val vertxContext: Context
        get() = VertxImpl.context() ?: throw IllegalStateException("Can't use CurrentVertx if not in a vertx-supplied thread")
    
    override fun <T> interceptContinuation(continuation: Continuation<T>) = VertxContinuation(vertxContext, continuation)
    
    override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
        vertxContext.owner().setTimer(unit.toMillis(time)) {
            continuation.resume(Unit) 
        }   
    }
}

inline suspend fun <T> vx(crossinline callback: (Handler<AsyncResult<T>>) -> Unit) = suspendCoroutine<T> { cont ->
    callback(Handler { result: AsyncResult<T> ->
        try {
            if (result.succeeded()) {
                cont.resume(result.result())
            } else {
                cont.resumeWithException(result.cause())
            }
        } catch(e: Exception) {
            cont.resumeWithException(e)
        }
    })
}

inline suspend fun <T> vxm(crossinline callback: (Handler<AsyncResult<Message<T>>>) -> Unit) = suspendCoroutine<T> { cont ->
    callback(Handler { result: AsyncResult<Message<T>> ->
        try {
            if (result.succeeded()) {
                cont.resume(result.result().body())
            } else {
                cont.resumeWithException(result.cause())
            }
        } catch(e: Exception) {
            cont.resumeWithException(e)
        }
    })
}

inline suspend fun vxHttp(crossinline callback: (Handler<AsyncResult<HttpResponse<Buffer>>>) -> Unit) = vx(callback)

suspend fun <TReply> EventBus.typedSendAsync(prefix: String = "", obj: Any) = vx<Message<TReply>> { 
    typedSend(prefix, obj, it)
}

suspend fun <TReply> EventBus.sendAsync(address: String, obj: Any) = vx<Message<TReply>> {
    send(address, obj, it)
}

suspend fun <TReReply> Message<out Any>.replyAsync(obj: Any) = vx<Message<TReReply>> {
    reply<TReReply>(obj, it)
}