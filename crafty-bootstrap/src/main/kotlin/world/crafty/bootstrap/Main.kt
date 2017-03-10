package world.crafty.bootstrap

import io.vertx.core.Vertx
import world.crafty.pc.world.anvil.loadWorld
import world.crafty.pc.PcConnectionServer
import world.crafty.pe.PeConnectionServer

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val before = System.currentTimeMillis()
    //val world = loadWorld("C:/Development/Minecraft/Crafty/neus")
    val elapsed = System.currentTimeMillis() - before
    println("world loading: $elapsed ms")

    //val pcServer = PcConnectionServer(25565, world)
    //vertx.deployVerticle(pcServer)
    
    val peServer = PeConnectionServer(19132)
    vertx.deployVerticle(peServer)

    println("All bootstrapped!")
}