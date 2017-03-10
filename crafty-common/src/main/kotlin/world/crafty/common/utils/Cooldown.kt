package world.crafty.common.utils

import java.time.Duration
import java.time.Instant

class Cooldown(val cooldown: Duration) {
    constructor(cooldownMs: Long) : this(Duration.ofMillis(cooldownMs))
    
    private var lastUse = Instant.MIN
    
    val ready get() = elapsed > cooldown

    fun use() {
        lastUse = Instant.now()
    }
    
    val remaining get() = Duration.between(expectedReady, Instant.now())
    
    val expectedReady: Instant
        get() = lastUse + cooldown
    
    val elapsed: Duration
        get() = Duration.between(Instant.now(), lastUse)
}