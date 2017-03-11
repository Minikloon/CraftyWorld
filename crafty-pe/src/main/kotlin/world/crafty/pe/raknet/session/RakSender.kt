package world.crafty.pe.raknet.session

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.EvictingQueue
import world.crafty.common.utils.average
import world.crafty.common.utils.kotlin.firstOrCompute
import world.crafty.pe.raknet.*
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.Instant
import java.util.*

class RakSender(val session: RakNetworkSession) {
    private var datagramSeqNo: Int = 0
    private var messageSeqNo: Int = 0
    private var messageOrderIndex: Int = 0
    private var messageSplitId: Short = 0

    private val mtuSize get() = session.mtuSize

    private val lastFewRoundTrips = EvictingQueue<Duration>(16)
    private val roundTripTime: Duration
        get() = if(lastFewRoundTrips.size == 0) Duration.ofMillis(200) else lastFewRoundTrips.average()

    private var datagramsPerSendLoop = 16
    private var lastNack = Instant.MIN

    private val datagramSendQueue: Queue<RakDatagram> = LinkedList<RakDatagram>()
    private val needAcks = mutableMapOf<Int, RakSentDatagram>()

    fun registerWithVertx(vertx: Vertx) {
        vertx.setPeriodic(10) { _ -> processDatagramSendQueue() }
        vertx.setPeriodic(40) { _ -> processResends() }
    }
    
    fun acknowledgeDatagram(seqNo: Int) {
        val acked = needAcks.remove(seqNo) ?: return
        lastFewRoundTrips.add(acked.sinceLastSend)
    }
    
    fun notifyNotAcknowledge() {
        if(Duration.between(Instant.now(), lastNack) > roundTripTime)
            datagramsPerSendLoop = Math.max(1, (datagramsPerSendLoop * 0.8).toInt())
        lastNack = Instant.now()
    }

    fun sendPayload(data: ByteArray, reliability: RakMessageReliability) {
        val message = RakMessage(
                RakMessageFlags(reliability),
                if(reliability.reliable) RakMessage.MetaReliability(messageSeqNo++) else null,
                if(reliability.ordered) RakMessage.MetaOrder(messageOrderIndex++, channel = 0) else null,
                null,
                data
        )
        val datagrams = binpackMessagesInDatagrams(message)
        datagrams.forEach { queueDatagram(it) }
    }

    private fun binpackMessagesInDatagrams(vararg messages: RakMessage) : List<RakDatagram> {
        val maxSize = mtuSize - RakDatagram.headerSize

        val messagesWithSplits = splitLargeMessages(messages, maxSize)
        messagesWithSplits.sortedByDescending { it.size }

        val groups = mutableListOf<ByteArrayOutputStream>()
        messagesWithSplits.forEach { message ->
            val group = groups.firstOrCompute(
                    { it.size() + message.size <= maxSize },
                    { ByteArrayOutputStream(maxSize) }
            )
            message.serialize(MinecraftOutputStream(group))
        }

        return groups.mapIndexed { index, bs ->
            val headerFlags = if(index == 0) RakDatagramFlags.nonContinuousUserDatagram else RakDatagramFlags.continuousUserDatagram
            RakDatagram(headerFlags, datagramSeqNo++, bs.toByteArray())
        }
    }

    private fun splitLargeMessages(packets: Array<out RakMessage>, maxSize: Int) : List<RakMessage> {
        return packets.flatMap { packet ->
            if(packet.size < maxSize) listOf(packet)
            else {
                val splitId = messageSplitId++
                var splitsCount = Math.ceil((packet.size.toDouble() / maxSize)).toInt()
                val overheadFromSplitting = splitsCount * RakMessage.MetaSplits.size
                if(packet.size + overheadFromSplitting > splitsCount * maxSize) ++splitsCount

                (0 until splitsCount).map { splitIndex ->
                    val dataIndex = splitIndex * (maxSize - RakMessage.MetaSplits.size)
                    val splitData = packet.data.copyOfRange(
                            dataIndex,
                            dataIndex + Math.min(maxSize, packet.data.size - dataIndex))
                    RakMessage(
                            RakMessageFlags(packet.headerFlags.reliability, hasSplit = true),
                            RakMessage.MetaReliability(messageSeqNo++),
                            packet.order,
                            RakMessage.MetaSplits(splitsCount, splitId, splitIndex),
                            splitData
                    )
                }
            }
        }
    }

    private fun queueDatagram(datagram: RakDatagram) {
        datagramSendQueue.add(datagram)
        var sent = needAcks[datagram.sequenceNumber]
        if(sent == null) {
            sent = RakSentDatagram(datagram, session)
            needAcks[datagram.sequenceNumber] = sent
        }
        sent.incSend()
    }

    fun processResends() {
        val ackTimeoutMs = Math.max(10, roundTripTime.toMillis() * 2)
        needAcks.values.forEach { sentDatagram ->
            if(sentDatagram.sinceLastSend.toMillis() > ackTimeoutMs) {
                queueDatagram(sentDatagram.datagram)
            }
        }
    }

    fun processDatagramSendQueue() {
        if(System.currentTimeMillis() % 500 == 0L)
            datagramsPerSendLoop = Math.min(128, datagramsPerSendLoop + 1)
        (1..datagramsPerSendLoop).forEach {
            val datagram = datagramSendQueue.poll() ?: return
            session.sendRaw(datagram.serialized())
        }
    }
}