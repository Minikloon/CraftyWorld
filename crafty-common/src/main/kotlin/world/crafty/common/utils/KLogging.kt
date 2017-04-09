package world.crafty.common.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> getLogger() : Logger {
    return LoggerFactory.getLogger(T::class.java)
}

fun getLogger(name: String) : Logger {
    return LoggerFactory.getLogger(name)
}

inline fun Logger.trace(crossinline msg: () -> String) {
    if(isTraceEnabled)
        trace(msg())
}

inline fun Logger.debug(crossinline msg: () -> String) {
    if(isDebugEnabled)
        trace(msg())
}

inline fun Logger.info(crossinline msg: () -> String) {
    if(isInfoEnabled)
        info(msg())
}

inline fun Logger.warn(crossinline msg: () -> String) {
    if(isWarnEnabled)
        warn(msg())
}

inline fun Logger.error(crossinline msg: () -> String) {
    if(isErrorEnabled)
        error(msg())
}