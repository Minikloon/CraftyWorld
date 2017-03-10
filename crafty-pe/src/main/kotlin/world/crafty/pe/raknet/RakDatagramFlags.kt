package world.crafty.pe.raknet

import world.crafty.pe.raknet.RakDatagramFlags.CongestionControlType.*

class RakDatagramFlags(header: Byte) {
    val userPacket: Boolean
    val ack: Boolean
    val nack: Boolean
    val packetPair: Boolean
    val bAndAS: Boolean
    val continuousSend: Boolean
    val needsBAndAS: Boolean
    val packetId: Byte = header

    constructor(type: CongestionControlType, bAndAS: Boolean = false) : this(congestionControlParamsToHeader(type, bAndAS))

    constructor(packetPair: Boolean, continuousSend: Boolean, needsBandAS: Boolean)
            : this(directParamsToHeader(packetPair, continuousSend, needsBandAS))

    init {
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

    companion object {
        val continuousUserDatagram = RakDatagramFlags(false, true, false)
        val nonContinuousUserDatagram = RakDatagramFlags(false, false, true)

        private fun Boolean.toInt() : Int {
            return if(this) 1 else 0
        }

        private fun isBit1(byte: Byte, indexFromRight: Int) : Boolean {
            val shifted = byte.toInt() ushr indexFromRight
            return (shifted and 0b1) == 1
        }

        private fun directParamsToHeader(packetPair: Boolean, continuousSend: Boolean, needsBandAS: Boolean) : Byte {
            return ((1 shl 7) or (packetPair.toInt() shl 4) or (continuousSend.toInt() shl 3) or (needsBandAS.toInt() shl 2)).toByte()
        }
        
        private fun congestionControlParamsToHeader(type: CongestionControlType, bAndAS: Boolean) : Byte {
            var b = (1 shl 7)
            when(type) {
                ACK -> {
                    b = b or (1 shl 6) or (bAndAS.toInt() shl 5)
                }
                NAK -> {
                    b = b or (1 shl 5)
                }
            }
            return b.toByte()
        }
    }

    enum class CongestionControlType {
        ACK,
        NAK
    }
}