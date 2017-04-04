package world.crafty.bootstrap

import io.vertx.core.Vertx
import world.crafty.server.world.anvil.loadWorld
import world.crafty.pc.PcConnectionServer
import world.crafty.server.world.World
import world.crafty.pe.PeConnectionServer
import world.crafty.server.CraftyServer
import world.crafty.skinpool.CraftySkinPoolServer
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val skinPool = try {
        CraftySkinPoolServer.startFromConsole()
    } catch(e: Exception) {
        e.printStackTrace()
        System.exit(1)
        return
    }

    vertx.deployVerticle(skinPool)

    val world = try {
        val directory = "neus"
        timedLoadWorld(directory)
    } catch(e: Exception) {
        e.printStackTrace()
        System.exit(1)
        return
    }

    val craftyServer = CraftyServer("worldServer:test", world)
    vertx.deployVerticle(craftyServer)

    startPc(vertx, craftyServer)

    startPe(vertx, craftyServer)

    println("All bootstrapped!")
}

fun timedLoadWorld(directory: String) : World {
    val before = System.currentTimeMillis()
    val world = loadWorld(directory)
    val elapsed = System.currentTimeMillis() - before
    println("World loaded in $elapsed ms")
    return world
}

fun startPc(vertx: Vertx, craftyServer: CraftyServer) {
    val elapsed = measureTimeMillis {
        val pcServer = PcConnectionServer(25565, craftyServer.address)
        vertx.deployVerticle(pcServer)
    }
    println("Started Pc connserver in $elapsed ms.")
}

fun startPe(vertx: Vertx, craftyServer: CraftyServer) {
    val elapsed = measureTimeMillis {
        val peServer = PeConnectionServer(19132, craftyServer.address)
        vertx.deployVerticle(peServer)
    }
    println("Started Pe connserver in $elapsed ms.")
}