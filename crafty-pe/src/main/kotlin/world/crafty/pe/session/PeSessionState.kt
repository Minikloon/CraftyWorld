package world.crafty.pe.session

import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import kotlinx.coroutines.experimental.launch
import world.crafty.common.utils.logger
import world.crafty.common.vertx.CurrentVertx
import world.crafty.common.vertx.typedConsumer
import world.crafty.pe.PeNetworkSession
import world.crafty.pe.proto.PePacket
import kotlin.reflect.KClass

abstract class PeSessionState(val session: PeNetworkSession) {
    val vertx: Vertx = session.vertx

    private val timerIds = mutableSetOf<Long>()
    private val consumers = mutableSetOf<MessageConsumer<*>>()

    suspend fun start() {
        onStart()
    }

    suspend open protected fun onStart() {}

    suspend abstract fun handle(packet: PePacket)

    fun unexpectedPacket(packet: PePacket) {
        logger(this).warn { "${session.address} sent an unexpected packet of type ${packet::class.simpleName} during login!" }
        session.disconnect("Connection error")
    }

    fun setPeriodic(millis: Long, action: suspend () -> Unit) {
        val timerId = session.server.vertx.setPeriodic(millis) {
            launch(CurrentVertx) {
                action()
            }
        }
        timerIds.add(timerId)
    }

    fun <T: Any> vertxTypedConsumer(prefix: String, clazz: KClass<T>, onReceive: (packet: Message<T>) -> Unit) {
        val consumer = vertx.eventBus().typedConsumer(prefix, clazz, onReceive)
        consumers.add(consumer)
    }
    
    suspend fun stop() {
        timerIds.forEach {
            vertx.cancelTimer(it)
        }
        consumers.forEach { it.unregister() }
        onStop()
    }
    
    suspend open fun onDisconnect(message: String) {}

    suspend open protected fun onStop() {}
}