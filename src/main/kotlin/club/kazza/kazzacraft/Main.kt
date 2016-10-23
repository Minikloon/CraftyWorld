package club.kazza.kazzacraft;

import club.kazza.kazzacraft.nbt.tags.NbtCompound
import club.kazza.kazzacraft.nbt.tags.NbtInt
import club.kazza.kazzacraft.nbt.tags.NbtString
import club.kazza.kazzacraft.network.MinecraftServer
import io.vertx.core.Vertx

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val server = MinecraftServer(25565)
    vertx.deployVerticle(server)

    println("Shard running")

    val compound = NbtCompound("root", listOf(
            NbtInt("age", 27),
            NbtString("name", "Sam")
    ))
    println(compound)
}