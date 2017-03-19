package world.crafty.common.vertx

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.impl.VertxImpl
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

object VertxContext : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val vertxContext = VertxImpl.context() ?: throw IllegalStateException("Can't use VertxContext if not in a vertx-supplied thread")
        vertxContext.runOnContext { block.run() }
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