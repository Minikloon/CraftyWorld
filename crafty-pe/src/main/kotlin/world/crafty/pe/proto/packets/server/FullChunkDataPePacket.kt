package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class FullChunkDataPePacket(
        val x: Int,
        val z: Int,
        val data: ByteArray
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x3a
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is FullChunkDataPePacket) throw IllegalArgumentException()
            stream.writeZigzagVarInt(obj.x)
            stream.writeZigzagVarInt(obj.z)
            stream.writeUnsignedVarInt(obj.data.size)
            stream.write(obj.data)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return FullChunkDataPePacket(
                    x = stream.readZigzagVarInt(),
                    z = stream.readZigzagVarInt(),
                    data = stream.readByteArray(stream.readUnsignedVarInt())
            )
        }
    }
}