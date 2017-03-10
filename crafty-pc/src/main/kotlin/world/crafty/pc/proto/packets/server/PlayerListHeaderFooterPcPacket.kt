package world.crafty.pc.proto.packets.server

import io.vertx.core.json.Json
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.McChat
import world.crafty.pc.proto.PcPacket

class PlayerListHeaderFooterPcPacket(
        val header: McChat,
        val footer: McChat
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x47
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is PlayerListHeaderFooterPcPacket) throw IllegalArgumentException()
            stream.writeJson(obj.header)
            stream.writeJson(obj.footer)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return PlayerListHeaderFooterPcPacket(
                    header = Json.decodeValue(stream.readString(), McChat::class.java),
                    footer = Json.decodeValue(stream.readString(), McChat::class.java)
            )
        }
    }
}