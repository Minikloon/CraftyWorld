package club.kazza.kazzacraft.world

import java.time.Duration

fun Iterable<Duration>.average() : Duration {
    return Duration.ofNanos(map { it.toNanos() }.average().toLong())
}