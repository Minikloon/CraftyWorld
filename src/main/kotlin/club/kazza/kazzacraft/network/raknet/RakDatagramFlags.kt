package club.kazza.kazzacraft.network.raknet

class RakDatagramFlags(header: Byte) {
    val userPacket: Boolean
    val ack: Boolean
    val nack: Boolean
    val packetPair: Boolean
    val bAndAS: Boolean
    val continuousSend: Boolean
    val needsBAndAS: Boolean
    val packetId: Int

    init {
        packetId = header.toInt()
        userPacket = isBit1(header, 7)
        ack = isBit1(header, 6)
        if(ack) {
            nack = false
            packetPair = false
            bAndAS = isBit1(header, 5)
            continuousSend = false
            needsBAndAS = false
        }
        else {
            bAndAS = false
            nack = isBit1(header, 5)
            if(nack) {
                packetPair = false
                continuousSend = false
                needsBAndAS = false
            }
            else {
                packetPair = isBit1(header, 4)
                continuousSend = isBit1(header, 3)
                needsBAndAS = isBit1(header, 2)
            }
        }
    }

    private fun isBit1(byte: Byte, littleEndianIndex: Int) : Boolean {
        val shifted = byte.toInt() ushr littleEndianIndex
        return (shifted and 0b1) == 1
    }
}