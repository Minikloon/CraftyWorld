package club.kazza.kazzacraft.network.raknet

class RakMessageFlags(val header: Byte) {
    val reliability: RaknetReliability
    val hasSplit: Boolean

    init {
        val reliabilityId = (header.toInt() ushr 5) and 0b111
        reliability = RaknetReliability.values()[reliabilityId]
        hasSplit = (header.toInt() and 0b10000) > 0
    }
}