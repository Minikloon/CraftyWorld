package world.crafty.pe.session

import io.vertx.core.Vertx
import kotlinx.coroutines.experimental.launch
import world.crafty.common.utils.logger
import world.crafty.common.vertx.CurrentVertx
import world.crafty.pe.PeNetworkSession
import world.crafty.pe.proto.PePacket

abstract class PeSessionState(val session: PeNetworkSession) {
    val vertx: Vertx = session.vertx

    private val timerIds = mutableSetOf<Long>()

    suspend fun start() {
        onStart()
    }

    suspend open protected fun onStart() {}

    suspend abstract fun handle(packet: PePacket)

    fun unexpectedPacket(packet: PePacket) {
        logger(this).warn { "${session.address} sent an unexpected packet of type ${packet::class.simpleName} during login!" }
        session.close()
    }

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