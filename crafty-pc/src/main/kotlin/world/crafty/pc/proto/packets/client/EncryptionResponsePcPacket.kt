package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class EncryptionResponsePcPacket(
        val sharedSecret: ByteArray,
        val verifyToken: ByteArray
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x01
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is EncryptionResponsePcPacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.sharedSecret.size)
            stream.write(obj.sharedSecret)
            stream.writeUnsignedVarInt(obj.verifyToken.size)
            stream.write(obj.verifyToken)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            val sharedSecret = ByteArray(stream.readUnsignedVarInt())
            stream.readFully(sharedSecret)
            val verifyToken = ByteArray(stream.readUnsignedVarInt())
            stream.readFully(verifyToken)
            return EncryptionResponsePcPacket(
                    sharedSecret = sharedSecret,
                    verifyToken = verifyToken
            )
        }
    }
}