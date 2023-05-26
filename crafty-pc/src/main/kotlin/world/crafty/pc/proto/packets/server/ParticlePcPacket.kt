package world.crafty.pc.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class ParticlePcPacket(
        val type: Int,
        val longDistance: Boolean,
        val x: Float,
        val y: Float,
        val z: Float,
        val offsetX: Float,
        val offsetY: Float,
        val offsetZ: Float,
        val data: Float,
        val count: Int,
        val moreData: Array<Int>
) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PcPacketCodec() {
        override val id = 0x23
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is ParticlePcPacket) throw IllegalArgumentException()
            stream.writeInt(obj.type)
            stream.writeBoolean(obj.longDistance)
            stream.writeFloat(obj.x)
            stream.writeFloat(obj.y)
            stream.writeFloat(obj.z)
            stream.writeFloat(obj.offsetX)
            stream.writeFloat(obj.offsetY)
            stream.writeFloat(obj.offsetZ)
            stream.writeFloat(obj.data)
            stream.writeInt(obj.count)
            for(x in obj.moreData) {
                stream.writeSignedVarInt(x)
            }
        }
        override fun deserialize(stream: MinecraftInputStream): PcPacket {
            return ParticlePcPacket(
                    type = stream.readInt(),
                    longDistance = stream.readBoolean(),
                    x = stream.readFloat(),
                    y = stream.readFloat(),
                    z = stream.readFloat(),
                    offsetX = stream.readFloat(),
                    offsetY = stream.readFloat(),
                    offsetZ = stream.readFloat(),
                    data = stream.readFloat(),
                    count = stream.readInt(),
                    moreData = arrayOf() // TODO: ??
            )
        }
    }
}