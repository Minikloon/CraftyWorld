package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class HandshakePcPacket(
        val protocolVersion: Int,
        val serverAddress: String,
        val port: Int,
        val nextState: Int
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x00
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is HandshakePcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.protocolVersion)
            stream.writeSignedString(obj.serverAddress)
            stream.writeShort(obj.port)
            stream.writeSignedVarInt(obj.nextState)
        }
        override fun deserialize(stream: MinecraftInputStream) : PcPacket {
            return HandshakePcPacket(
                    protocolVersion = stream.readSignedVarInt(),
                    serverAddress = stream.readSignedString(),
                    port = stream.readUnsignedShort(),
                    nextState = stream.readSignedVarInt()
            )
        }
    }
}