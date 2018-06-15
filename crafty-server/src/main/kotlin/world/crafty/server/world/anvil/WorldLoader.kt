package world.crafty.server.world.anvil

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.utils.NibbleArray
import world.crafty.common.Location
import world.crafty.nbt.NbtInputStream
import world.crafty.nbt.tags.*
import world.crafty.proto.CraftyChunk
import world.crafty.proto.CraftyChunkColumn
import world.crafty.server.world.World
import java.io.File
import java.io.FileInputStream
import java.util.stream.Collectors
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

fun loadWorld(folder: String) : World {
    val levelStream = NbtInputStream(GZIPInputStream(FileInputStream("$folder/level.dat")))
    val levelCompound = (levelStream.readTag() as NbtCompound)["Data"] as NbtCompound
    val spawnPoint = Location(
            (levelCompound["SpawnX"] as NbtInt).value.toFloat(),
            (levelCompound["SpawnY"] as NbtInt).value.toFloat(),
            (levelCompound["SpawnZ"] as NbtInt).value.toFloat()
    )

    val regions = File("$folder/region").listFiles()
            .filter { it.path.endsWith("mca") }
            .map(::readRegion)
    val chunks = regions.flatten()

    return World(chunks, spawnPoint)
}

private val sectorSize = 4096
data class ChunkEntry(val offset: Int, val paddedSize: Int)
fun readRegion(file: File) : List<CraftyChunkColumn> {
    val regionBytes = file.readBytes()
    val regionStream = MinecraftInputStream(regionBytes)
    val chunkEntries = (0 until 1024).map {
        val entry = regionStream.readInt()
        val chunkOffset = entry ushr 8
        val chunkSize = entry and 0xF
        ChunkEntry(chunkOffset * sectorSize, chunkSize * sectorSize)
    }.filterNot { it.offset == 0 && it.paddedSize == 0 }

    return chunkEntries.parallelStream().map {
        val headerStream = MinecraftInputStream(regionBytes, it.offset, it.paddedSize)
        val chunkSize = headerStream.readInt() - 1 // - 1 because of the compressionScheme byte
        val compressionScheme = headerStream.readByte().toInt()
        val chunkStream = MinecraftInputStream(regionBytes, it.offset + 5, chunkSize)
        val compressionStream = if(compressionScheme == 1) GZIPInputStream(chunkStream) else InflaterInputStream(chunkStream)
        val chunkNbtStream = NbtInputStream(compressionStream)
        val chunkCompound = (chunkNbtStream.readTag() as NbtCompound)["Level"] as NbtCompound
        readChunk(chunkCompound)
    }.collect(Collectors.toList<CraftyChunkColumn>())
}

fun readChunk(nbt: NbtCompound) : CraftyChunkColumn {
    val chunkX = (nbt["xPos"] as NbtInt).value
    val chunkZ = (nbt["zPos"] as NbtInt).value

    val biomes = (nbt["Biomes"] as NbtByteArray).value
    val chunks = arrayOfNulls<CraftyChunk>(16)

    (nbt["Sections"] as NbtList).forEach {
        val sectionCompound = it as NbtCompound
        val sectionY = (sectionCompound["Y"] as NbtByte).value

        val blocksId = (sectionCompound["Blocks"] as NbtByteArray).value
        val blocksData = NibbleArray((sectionCompound["Data"] as NbtByteArray).value)
        
        val blockLight = NibbleArray((sectionCompound["BlockLight"] as NbtByteArray).value)
        val skyLight = NibbleArray((sectionCompound["SkyLight"] as NbtByteArray).value)
        
        val section = CraftyChunk(blocksId, blocksData, blockLight, skyLight)

        chunks[sectionY] = section
    }

    return CraftyChunkColumn(chunkX, chunkZ, chunks, biomes)
}

fun streamFromNbt(compound: NbtCompound, tag: String) : MinecraftInputStream {
    return MinecraftInputStream((compound[tag] as NbtByteArray).value)
}