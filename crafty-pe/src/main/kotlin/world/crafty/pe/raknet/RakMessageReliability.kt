package world.crafty.pe.raknet

enum class RakMessageReliability(val id: Int, val reliable: Boolean, val ordered: Boolean) {
    UNRELIABLE(0, false, false),
    UNRELIABLE_SEQUENCED(1, false, true),
    RELIABLE(2, true, false),
    RELIABLE_ORDERED(3, true, true),
    RELIABLE_SEQUENCED(4, true, true),
    UNRELIABLE_ACK_RECEIPT(5, false, false),
    RELIABLE_ACK_RECEIPT(6, true, false),
    RELIABLE_OREDERED_ACK_RECEIPT(7, true, true)
}