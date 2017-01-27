package club.kazza.kazzacraft.network.raknet

class RakMessageFlags(val header: Byte) {
    val reliability: RakMessageReliability
    val hasSplit: Boolean

    constructor(reliability: RakMessageReliability, hasSplit: Boolean) : this({
        ((reliability.id shl 5) or (if(hasSplit) (1 shl 4) else 0)).toByte()
    }.invoke())

    init {
        val reliabilityId = (header.toInt() ushr 5) and 0b111
        reliability = RakMessageReliability.values()[reliabilityId]
        hasSplit = (header.toInt() and 0b10000) > 0
    }
}