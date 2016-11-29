package club.kazza.kazzacraft.network.raknet

enum class RaknetReliability(val id: Int, val reliable: Boolean) {
    UNRELIABLE(0, false),
    UNRELIABLE_SEQUENCED(1, false),
    RELIABLE(2, true),
    RELIABLE_ORDERED(3, true),
    RELIABLE_SEQUENCED(4, true),
    UNRELIABLE_ACK_RECEIPT(5, false),
    RELIABLE_ACK_RECEIPT(6, true),
    RELIABLE_OREDERED_ACK_RECEIPT(7, true)
}