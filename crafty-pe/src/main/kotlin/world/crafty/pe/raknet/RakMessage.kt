package world.crafty.pe.raknet

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PeCodec

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
    
    data class MetaReliability(val reliableId: Int) { companion object { val size = 3 } }
    data class MetaOrder(val index: Int, val channel: Byte) { companion object { val size = 4 } }
    data class MetaSplits(val splitsCount: Int, val splitsId: Short, val splitsIndex: Int) { companion object { val size = 10 } }
    
    private fun nullOr(obj: Any?, or: Int) : Int { return if(obj == null) 0 else or }
    val headerSize by lazy {
        3 + nullOr(reliability, MetaReliability.size) + nullOr(order, MetaOrder.size) + nullOr(splits, MetaSplits.size)
    }
    val size by lazy { headerSize + data.size }
    
    fun serialize(stream: MinecraftOutputStream) {
        Codec.serialize(this, stream)
    }
    
    val overheadPerSplit : Int
        get() {
            val reliabilityOverhead = if(reliability == null) 0 else MetaReliability.size
            val orderOverhead = if(order == null) 0 else MetaOrder.size
            val splitOverhead = MetaSplits.size
            return reliabilityOverhead + orderOverhead + splitOverhead
        }

    object Codec : PeCodec<RakMessage> {
        override fun serialize(obj: RakMessage, stream: MinecraftOutputStream) {
            stream.writeByte(obj.headerFlags.header)
            stream.writeShort(obj.data.size * 8)
            
            if(obj.reliability != null)
                stream.write3BytesInt(obj.reliability.reliableId)
            if(obj.order != null) {
                stream.write3BytesInt(obj.order.index)
                stream.writeByte(obj.order.channel)
            }
            if(obj.splits != null) {
                stream.writeInt(obj.splits.splitsCount)
                stream.writeShort(obj.splits.splitsId.toInt())
                stream.writeInt(obj.splits.splitsIndex)
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
                        MetaReliability(
                                reliableId = stream.read3BytesInt()
                        )
                    else null


            val orderMeta =
                    if(reliability.ordered)
                        MetaOrder(
                                index = stream.read3BytesInt(),
                                channel = stream.readByte()
                        )
                    else null

            val splitsMeta =
                    if(header.hasSplit)
                        MetaSplits(
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