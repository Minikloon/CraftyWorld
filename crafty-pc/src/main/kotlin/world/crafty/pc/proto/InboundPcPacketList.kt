package world.crafty.pc.proto

abstract class InboundPcPacketList {
    protected abstract fun getCodecs() : List<PcPacket.PcPacketCodec>
    val idToCodec: Map<Int, PcPacket.PcPacketCodec>
    init {
        val mappedPackets = mutableMapOf<Int, PcPacket.PcPacketCodec>()
        for(codec in getCodecs())
            mappedPackets[codec.id] = codec
        idToCodec = mappedPackets
    }
}