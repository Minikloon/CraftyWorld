package world.crafty.pc.session

import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import kotlinx.coroutines.experimental.launch
import world.crafty.common.utils.logger
import world.crafty.common.vertx.CurrentVertx
import world.crafty.common.vertx.typedConsumer
import world.crafty.pc.PcNetworkSession
import world.crafty.pc.proto.InboundPcPacketList
import world.crafty.pc.proto.PcPacket
import kotlin.reflect.KClass

abstract class PcSessionState(val session: PcNetworkSession) { // TODO: Too similar to Verticle
    val vertx: Vertx = session.connServer.vertx
    abstract val packetList: InboundPcPacketList
    
    private val timerIds = mutableSetOf<Long>()
    private val consumers = mutableSetOf<MessageConsumer<*>>()
    
    private var started = false
    private var stopped = false
    
    suspend fun start() {
        if(started || stopped)
            return
        started = true
        
        onStart()
    }
    
    suspend open protected fun onStart() {}
    
    suspend abstract fun handle(packet: PcPacket)

    fun unexpectedPacket(packet: PcPacket) {
        logger(this).warn { "${session.address} sent an unexpected packet of type ${packet::class.simpleName} during login!" }
        session.disconnect("Connection error")
    }
    
    fun setPeriodic(millis: Long, action: suspend () -> Unit) {
        val timerId = session.connServer.vertx.setPeriodic(millis) { 
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
        if(stopped || !started)
            return
        stopped = true
        
        timerIds.forEach {
            vertx.cancelTimer(it)
        }
        consumers.forEach(MessageConsumer<*>::unregister)
        onStop()
    }
    
    suspend open fun onDisconnect(message: String) {}
    
    suspend open protected fun onStop() {}
}