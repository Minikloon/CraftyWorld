package world.crafty.pe.proto.packets.mixed

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class EncryptionWrapperPePacket(
        val payload: ByteArray
) : PePacket() {
    constructor(payload: PePacket) : this(payload.serializedWithId())
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0xFE
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is EncryptionWrapperPePacket) throw IllegalArgumentException()
            stream.write(obj.payload)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return EncryptionWrapperPePacket(
                    payload = stream.readRemainingBytes()
            )
        }
    }
}