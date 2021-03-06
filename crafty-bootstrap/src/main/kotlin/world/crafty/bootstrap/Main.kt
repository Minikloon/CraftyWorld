package world.crafty.bootstrap

import io.vertx.core.Vertx
import world.crafty.common.utils.logger
import world.crafty.server.world.anvil.loadWorld
import world.crafty.pc.PcConnectionServer
import world.crafty.server.world.World
import world.crafty.pe.PeConnectionServer
import world.crafty.server.CraftyServer
import world.crafty.skinpool.CraftySkinPoolServer
import kotlin.system.measureTimeMillis

private val log = logger("crafty-bootstrap")
fun main(args: Array<String>) {
    writeLogo()
    bootstrapLogging()
    val vertx = Vertx.vertx()

    val world = try {
        val directory = "lobby"
        timedLoadWorld(directory)
    } catch(e: Exception) {
        e.printStackTrace()
        System.exit(1)
        return
    }

    val skinPool = try {
        CraftySkinPoolServer.startFromConsole()
    } catch(e: Exception) {
        e.printStackTrace()
        System.exit(1)
        return
    }

    vertx.deployVerticle(skinPool)

    val craftyServer = CraftyServer("worldServer:test", world)
    vertx.deployVerticle(craftyServer)

    startPc(vertx, craftyServer)

    startPe(vertx, craftyServer)

    log.info { "All bootstrapped!" }
}

fun timedLoadWorld(directory: String) : World {
    val before = System.currentTimeMillis()
    val world = loadWorld(directory)
    val elapsed = System.currentTimeMillis() - before
    log.info { "World loaded in $elapsed ms" }
    return world
}

fun startPc(vertx: Vertx, craftyServer: CraftyServer) {
    val elapsed = measureTimeMillis {
        val pcServer = PcConnectionServer(25565, craftyServer.address)
        vertx.deployVerticle(pcServer)
    }
    log.info { "Started Pc connserver in $elapsed ms." }
}

fun startPe(vertx: Vertx, craftyServer: CraftyServer) {
    val elapsed = measureTimeMillis {
        val peServer = PeConnectionServer(19133, craftyServer.address)
        vertx.deployVerticle(peServer)
    }
    log.info { "Started Pe connserver in $elapsed ms." }
}

private fun bootstrapLogging() {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
}

class Bootstrapper
private fun writeLogo() {
    Bootstrapper::class.java.getResourceAsStream("/ascii-logo.txt").reader().useLines { 
        it.forEach { log.info(it) }
    }
}