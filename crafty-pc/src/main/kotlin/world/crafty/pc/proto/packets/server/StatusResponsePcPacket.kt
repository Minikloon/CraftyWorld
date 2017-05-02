package world.crafty.pc.proto.packets.server

import io.vertx.core.json.Json
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class StatusResponsePcPacket(
        val version: ServerVersion,
        val players: PlayerStatus,
        val description: Description
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x00
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is StatusResponsePcPacket) throw IllegalArgumentException()
            stream.writeSignedString(Json.encode(obj))
        }
        override fun deserialize(stream: MinecraftInputStream) : PcPacket {
            val fromJson = Json.decodeValue(stream.readSignedString(), StatusResponsePcPacket::class.java)
            return StatusResponsePcPacket(
                    version = fromJson.version,
                    players = fromJson.players,
                    description = fromJson.description
            )
        }
    }
    data class ServerVersion(
            val name: String,
            val protocol: Int
    )
    data class PlayerStatus(
            val max: Int,
            val online: Int
    )
    data class Description(
            val text: String
    )
}