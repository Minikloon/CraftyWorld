package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket
import java.util.stream.IntStream

class SetPassengersPcPacket(
        val vehicleId: Int,
        val passengerIds: List<Int>
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x45
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetPassengersPcPacket) throw IllegalArgumentException()
            stream.writeSignedVarInt(obj.vehicleId)
            stream.writeSignedVarInt(obj.passengerIds.size)
            obj.passengerIds.forEach { id -> stream.writeSignedVarInt(id)}
        }

        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SetPassengersPcPacket(
                    vehicleId = stream.readSignedVarInt(),
                    passengerIds = (0 until stream.readSignedVarInt()).map { stream.readSignedVarInt() }
            )
        }
    }
}