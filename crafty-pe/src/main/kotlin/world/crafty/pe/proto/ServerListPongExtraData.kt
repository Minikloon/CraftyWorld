package world.crafty.pe.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import java.io.ByteArrayOutputStream

class ServerListPongExtraData(
        val motdFirstLine: String,
        val motdSecondLine: String,
        val version: String,
        val onlinePlayers: Int,
        val maxPlayers: Int,
        val serverId: Long
) {
    val str = "MCPE;$motdFirstLine;100;1.0.0.7;$onlinePlayers;$maxPlayers;$serverId;$motdSecondLine;Survival;"
    
    fun serialized() : ByteArray {
        return MinecraftOutputStream.serialized {
            Codec.serialize(this, it)
        }
    }
    
    object Codec : PeCodec<ServerListPongExtraData> {
        override fun serialize(obj: ServerListPongExtraData, stream: MinecraftOutputStream) {
            val str = obj.str
            stream.writeUTF(str)
        }
        override fun deserialize(stream: MinecraftInputStream): ServerListPongExtraData {
            val splits = stream.readUTF().split(";")
            return ServerListPongExtraData(
                    motdFirstLine = splits[1],
                    motdSecondLine = splits[7],
                    version = splits[3],
                    onlinePlayers = splits[4].toInt(),
                    maxPlayers = splits[5].toInt(),
                    serverId = splits[6].toLong()
            )
        }
    }
}