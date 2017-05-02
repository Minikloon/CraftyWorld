package world.crafty.common.utils

import java.time.Duration
import java.time.Instant

fun Iterable<Duration>.average() : Duration {
    return Duration.ofNanos(map { it.toNanos() }.average().toLong())
}

fun Instant.sinceThen() : Duration {
    return Duration.between(this, Instant.now())
}