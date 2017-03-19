package world.crafty.common.vertx

import io.vertx.core.AsyncResult
import io.vertx.core.Context
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.impl.VertxImpl
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Delay
import kotlinx.coroutines.experimental.DisposableHandle
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
        vertxContext.owner().setTimer(unit.toMillis(time)) { continuation.resume(Unit) }   
    }
}

inline suspend fun <T> vxm(crossinline callback: (Handler<AsyncResult<Message<T>>>) -> Unit) = suspendCoroutine<T> { cont ->
    callback(Handler { result: AsyncResult<Message<T>> ->
        if (result.succeeded()) {
            cont.resume(result.result().body())
        } else {
            cont.resumeWithException(result.cause())
        }
    })
}

inline suspend fun <T> vx(crossinline callback: (Handler<AsyncResult<T>>) -> Unit) = suspendCoroutine<T> { cont ->
    callback(Handler { result: AsyncResult<T> ->
        if (result.succeeded()) {
            cont.resume(result.result())
        } else {
            cont.resumeWithException(result.cause())
        }
    })
}