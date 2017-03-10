package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

class ServerHandshakePePacket(
        val serverKey: PublicKey,
        val token: ByteArray
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec

    object Codec : PePacketCodec() {
        override val id = 0x03
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ServerHandshakePePacket) throw IllegalArgumentException()
            val base64Key = Base64.getEncoder().encode(obj.serverKey.encoded)
            stream.writeInt(base64Key.size)
            stream.write(base64Key)
            stream.write(obj.token)
        }
        val keyFactory = KeyFactory.getInstance("EC")
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return ServerHandshakePePacket(
                    serverKey = keyFactory.generatePublic(X509EncodedKeySpec(stream.readByteArray(stream.readInt()))),
                    token = stream.readByteArray(stream.readInt())
            )
        }
    }
}