package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class EncryptionRequestPcPacket(
        val serverId: String,
        val publicKey: ByteArray,
        val verifyToken: ByteArray
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x01
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is EncryptionRequestPcPacket) throw IllegalArgumentException()
            stream.writeSignedString(obj.serverId)
            stream.writeSignedVarInt(obj.publicKey.size)
            stream.write(obj.publicKey)
            stream.writeSignedVarInt(obj.verifyToken.size)
            stream.write(obj.verifyToken)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            val serverId = stream.readSignedString()
            val publicKey = ByteArray(stream.readSignedVarInt())
            stream.readFully(publicKey)
            val verifyToken = ByteArray(stream.readSignedVarInt())
            stream.readFully(verifyToken)
            return EncryptionRequestPcPacket(
                    serverId = serverId,
                    publicKey = publicKey,
                    verifyToken = verifyToken
            )
        }
    }
}