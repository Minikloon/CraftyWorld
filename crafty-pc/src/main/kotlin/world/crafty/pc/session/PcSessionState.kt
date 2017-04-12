package world.crafty.pc.session

import io.vertx.core.Vertx
import kotlinx.coroutines.experimental.launch
import world.crafty.common.vertx.CurrentVertx
import world.crafty.pc.PcNetworkSession
import world.crafty.pc.proto.InboundPcPacketList
import world.crafty.pc.proto.PcPacket

abstract class PcSessionState(val session: PcNetworkSession) {
    val vertx: Vertx = session.server.vertx
    abstract val packetList: InboundPcPacketList
    
    private val timerIds = mutableSetOf<Long>()
    
    suspend fun start() {
        onStart()
    }
    
    suspend open protected fun onStart() {}
    
    suspend abstract fun handle(packet: PcPacket)
    
    fun setPeriodic(millis: Long, action: suspend () -> Unit) {
        val timerId = session.server.vertx.setPeriodic(millis) { 
            launch(CurrentVertx) {
                action()
            }
        }
        timerIds.add(timerId)
    }
    
    suspend fun stop() {
        timerIds.forEach {
            vertx.cancelTimer(it)
        }
        onStop()
    }
    
    suspend open protected fun onStop() {}
}