package world.crafty.pe.raknet.packets

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import world.crafty.pe.raknet.unconnectedBlabber

class OpenConnectionRequest1PePacket(
        val protocolVersion: Byte,
        val mtuSize: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec

    object Codec : PePacketCodec() {
        override val id = 0x05
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is OpenConnectionRequest1PePacket) throw IllegalArgumentException()
            stream.write(unconnectedBlabber)
            stream.writeByte(obj.protocolVersion)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            stream.skipBytes(unconnectedBlabber.size)
            val protocolVersion = stream.readByte()
            return OpenConnectionRequest1PePacket(
                    protocolVersion = protocolVersion,
                    mtuSize = computeMtuSize(stream)
            )
        }
        fun computeMtuSize(stream: MinecraftInputStream) : Int {
            val packetIdSize = 1
            val protocolVersionSize = 1
            return packetIdSize + protocolVersionSize + unconnectedBlabber.size + stream.available()
        }
    }
}
