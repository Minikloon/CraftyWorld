package world.crafty.common.utils;

import java.time.Duration
import java.time.Instant

class Clock(var start: Instant) {
    constructor() : this(Instant.now())

    val elapsed: Duration
        get() = Duration.between(Instant.now(), start)

    // returns elapsed ms
    fun reset() : Duration {
        val elapsed = elapsed
        start = Instant.now()
        return elapsed
    }
}