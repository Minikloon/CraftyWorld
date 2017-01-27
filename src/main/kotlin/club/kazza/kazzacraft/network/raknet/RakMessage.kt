package club.kazza.kazzacraft.network.raknet

import club.kazza.kazzacraft.network.protocol.PeCodec
import club.kazza.kazzacraft.network.serialization.MinecraftInputStream
import club.kazza.kazzacraft.network.serialization.MinecraftOutputStream

class RakMessage(val headerFlags: RakMessageFlags, val reliability: MetaReliability?, val order: MetaOrder?, val splits: MetaSplits?, val data: ByteArray) {
    init {
        if(headerFlags.reliability.reliable)
            requireNotNull(reliability)
        if(headerFlags.reliability.ordered)
            requireNotNull(order)
        if(headerFlags.hasSplit)
            requireNotNull(splits)
    }
    
    val dataAsStream: MinecraftInputStream
        get() = MinecraftInputStream(data)
    
    data class MetaReliability(val reliableId: Int)
    
    data class MetaOrder(val index: Int, val channel: Byte)
    
    data class MetaSplits(val splitsCount: Int, val splitsId: Short, val splitsIndex: Int)
    
    fun serialize(stream: MinecraftOutputStream) {
        Codec.serialize(this, stream)
    }

    object Codec : PeCodec<RakMessage> {
        override fun serialize(obj: RakMessage, stream: MinecraftOutputStream) {
            stream.writeByte(obj.headerFlags.header)
            stream.writeShort(obj.data.size * 8)

            val reliability = obj.headerFlags.reliability
            if(reliability.reliable)
                stream.write3BytesInt(obj.reliability!!.reliableId)
            if(reliability.ordered) {
                stream.write3BytesInt(obj.order!!.index)
                stream.writeByte(obj.order.channel)
            }

            stream.write(obj.data)
        }

        override fun deserialize(stream: MinecraftInputStream): RakMessage {
            val header = RakMessageFlags(stream.readByte())

            val bitsLength = stream.readUnsignedShort()
            val messageSize = Math.ceil(bitsLength.toDouble() / 8).toInt()

            val reliability = header.reliability

            val reliabilityMeta =
                    if(reliability.reliable)
                        RakMessage.MetaReliability(
                                reliableId = stream.read3BytesInt()
                        )
                    else null


            val orderMeta =
                    if(reliability.ordered)
                        RakMessage.MetaOrder(
                                index = stream.read3BytesInt(),
                                channel = stream.readByte()
                        )
                    else null

            val splitsMeta =
                    if(header.hasSplit)
                        RakMessage.MetaSplits(
                                splitsCount = stream.readInt(),
                                splitsId = stream.readShort(),
                                splitsIndex = stream.readInt()
                        )
                    else null

            val message = stream.readByteArray(messageSize)

            return RakMessage(header, reliabilityMeta, orderMeta, splitsMeta, message)
        }
    }
}