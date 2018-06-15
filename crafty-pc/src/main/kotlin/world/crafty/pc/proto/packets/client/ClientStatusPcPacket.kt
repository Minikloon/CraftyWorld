package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

enum class ClientStatus { RESPAWN, REQUEST_STATS, OPEN_INVENTORY }
class ClientStatusPcPacket(
        val action: ClientStatus
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x02
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ClientStatusPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.action.ordinal)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ClientStatusPcPacket(
                    action = ClientStatus.values()[stream.readSignedVarInt()]
            )
        }
    }
}