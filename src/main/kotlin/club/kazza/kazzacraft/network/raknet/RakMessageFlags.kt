package club.kazza.kazzacraft.network.raknet

class RakMessageFlags(val header: Byte) {
    val reliability: RaknetReliability
    val hasSplit: Boolean

    constructor(reliability: RaknetReliability, hasSplit: Boolean) : this({
        ((reliability.id shl 5) or (if(hasSplit) (1 shl 4) else 0)).toByte()
    }.invoke())

    init {
        val reliabilityId = (header.toInt() ushr 5) and 0b111
        reliability = RaknetReliability.values()[reliabilityId]
        hasSplit = (header.toInt() and 0b10000) > 0
    }
}