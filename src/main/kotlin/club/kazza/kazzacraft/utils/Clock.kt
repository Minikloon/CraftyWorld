package club.kazza.kazzacraft.utils;

class Clock {
    private var startMs = System.currentTimeMillis()

    val elapsedMs : Long
        get() = System.currentTimeMillis() - startMs

    val elapsedSeconds : Double
        get() = elapsedMs / 1000.0

    // returns elapsed ms
    fun reset() : Long {
        val elapsed = elapsedMs
        startMs = System.currentTimeMillis()
        return elapsed
    }
}