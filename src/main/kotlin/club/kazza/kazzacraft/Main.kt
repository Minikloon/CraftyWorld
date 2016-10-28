package club.kazza.kazzacraft;

import club.kazza.kazzacraft.network.MinecraftServer
import club.kazza.kazzacraft.world.anvil.loadWorld
import io.vertx.core.Vertx

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val before = System.currentTimeMillis()
    val world = loadWorld("neus")
    val elapsed = System.currentTimeMillis() - before
    println("world loading: $elapsed ms")

    val server = MinecraftServer(25565, world)
    vertx.deployVerticle(server)

    println("Shard running")
}