package club.kazza.kazzacraft;

import club.kazza.kazzacraft.network.PeConnectionServer
import io.vertx.core.Vertx

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val before = System.currentTimeMillis()
    //val world = loadWorld("neus")
    val elapsed = System.currentTimeMillis() - before
    println("world loading: $elapsed ms")

    //val pcServer = PcConnectionServer(25565, world)
    //vertx.deployVerticle(pcServer)

    val peServer = PeConnectionServer(19132)
    vertx.deployVerticle(peServer)
    
    println("Shard running")
}