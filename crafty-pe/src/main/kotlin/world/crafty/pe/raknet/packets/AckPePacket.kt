package world.crafty.pe.raknet.packets

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket
import java.util.ArrayList

class AckPePacket(
        val datagramSeqNos: List<Int>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0xc0
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is AckPePacket) throw IllegalArgumentException()
            val ranges = asRanges(obj.datagramSeqNos)
            stream.writeShort(ranges.count())
            ranges.forEach {
                val startEqualsEnd = it.first == it.last
                stream.writeBoolean(startEqualsEnd)
                if(startEqualsEnd) {
                    stream.write3BytesInt(it.first)
                }
                else {
                    stream.write3BytesInt(it.first)
                    stream.write3BytesInt(it.last)
                }
            }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val rangesCount = stream.readUnsignedShort()
            val ranges = ArrayList<IntRange>(rangesCount)
            repeat(rangesCount) {
                val startEqualsEnd = stream.readBoolean()
                if(startEqualsEnd) {
                    val datagramSeqNo = stream.read3BytesInt()
                    ranges.add(datagramSeqNo..datagramSeqNo)
                }
                else {
                    val first = stream.read3BytesInt()
                    val last = stream.read3BytesInt()
                    ranges.add(first..last)
                }
            }
            val seqNos = ranges.flatMap { it.asIterable() }
            return AckPePacket(seqNos)
        }
        private fun asRanges(ints: List<Int>) : List<IntRange> {
            var count = 0
            return ints
                    .groupBy { i -> count++ - i }.values
                    .map { it.first()..it.last() }
        }
    }
}