package world.crafty.pc.proto.packets.server

import org.joml.Vector3ic
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class SetSpawnPositionPcPacket(
        val loc: Vector3ic
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x43
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetSpawnPositionPcPacket) throw IllegalArgumentException()
            stream.writeBlockLocation(obj.loc)
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return SetSpawnPositionPcPacket(
                    loc = stream.readBlockLocation()
            )
        }
    }
}