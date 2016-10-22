package club.kazza.kazzacraft;

import club.kazza.kazzacraft.network.MinecraftServer
import io.vertx.core.Vertx

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val server = MinecraftServer(25565)
    vertx.deployVerticle(server)

    println("Shard running")
}