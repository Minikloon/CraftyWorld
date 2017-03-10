package world.crafty.pe.proto

abstract class InboundPePacketList {
    protected abstract fun getCodecs() : List<PePacket.PePacketCodec>
    val idToCodec: Map<Int, PePacket.PePacketCodec>
    init {
        val mappedPackets = mutableMapOf<Int, PePacket.PePacketCodec>()
        for(codec in getCodecs()) {
            val prev = mappedPackets.put(codec.id, codec)
            if(prev != null)
                throw IllegalStateException("${codec::class.qualifiedName} registered twice in ${this::class.simpleName}")
        }
        idToCodec = mappedPackets
    }
}