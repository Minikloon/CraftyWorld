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
            stream.writeSignedVarInt(obj.sharedSecret.size)
            stream.write(obj.sharedSecret)
            stream.writeSignedVarInt(obj.verifyToken.size)
            stream.write(obj.verifyToken)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            val sharedSecret = ByteArray(stream.readSignedVarInt())
            stream.readFully(sharedSecret)
            val verifyToken = ByteArray(stream.readSignedVarInt())
            stream.readFully(verifyToken)
            return EncryptionResponsePcPacket(
                    sharedSecret = sharedSecret,
                    verifyToken = verifyToken
            )
        }
    }
}