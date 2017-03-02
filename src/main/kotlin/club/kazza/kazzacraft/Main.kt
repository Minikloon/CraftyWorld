package club.kazza.kazzacraft;

import club.kazza.kazzacraft.network.PeConnectionServer
import club.kazza.kazzacraft.utils.CompressionAlgorithm
import club.kazza.kazzacraft.utils.decompress
import club.kazza.kazzacraft.utils.toHexStr
import club.kazza.kazzacraft.world.anvil.loadWorld
import io.vertx.core.Vertx
import java.util.zip.Deflater
import java.util.zip.Inflater

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val before = System.currentTimeMillis()
    val world = loadWorld("C:/Development/Minecraft/Kazza/Code/KazzaCraft/neus")
    val elapsed = System.currentTimeMillis() - before
    println("world loading: $elapsed ms")

    //val pcServer = PcConnectionServer(25565, world)
    //vertx.deployVerticle(pcServer)

    val peServer = PeConnectionServer(19132)
    vertx.deployVerticle(peServer)
    
    //testDecompress()
    
    println("Shard running")
}

fun testDecompress() {
    println(decompressParse("78 DA 63 E1 64 61 60 00 00 00 49 00 12"))
}

fun decompressParse(str: String) : String {
    val bytes = str
            .split(" ")
            .map { Integer.parseInt(it, 16).toByte() }
    val decompress = { bytes: List<Byte> ->
        val inflater = Inflater()
        inflater.setInput(bytes.toByteArray())
        val output = ByteArray(200)
        val size = inflater.inflate(output)
        output.copyOfRange(0, size)
    }
    return decompress(bytes).map(Byte::toHexStr).joinToString()
}