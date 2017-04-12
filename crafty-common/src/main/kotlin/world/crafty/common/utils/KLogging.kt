package world.crafty.common.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> logger() : KotlinLoggerWrapper {
    return KotlinLoggerWrapper(LoggerFactory.getLogger(T::class.java))
}

inline fun <reified T: Any> logger(obj: T) : KotlinLoggerWrapper {
    return KotlinLoggerWrapper(LoggerFactory.getLogger(T::class.java))
}

fun logger(name: String) : KotlinLoggerWrapper {
    return KotlinLoggerWrapper(LoggerFactory.getLogger(name))
}

class KotlinLoggerWrapper(val logger: Logger) : Logger by logger {
    inline fun trace(crossinline msg: () -> String) {
        if(logger.isTraceEnabled)
            logger.trace(msg())
    }

    inline fun debug(crossinline msg: () -> String) {
        if(logger.isDebugEnabled)
            logger.trace(msg())
    }

    inline fun info(crossinline msg: () -> String) {
        if(logger.isInfoEnabled)
            logger.info(msg())
    }

    inline fun warn(crossinline msg: () -> String) {
        if(logger.isWarnEnabled)
            logger.warn(msg())
    }

    inline fun error(crossinline msg: () -> String) {
        if(logger.isErrorEnabled)
            logger.error(msg())
    }
    
    inline fun error(e: Throwable, crossinline msg: () -> String) {
        if(logger.isErrorEnabled)
            logger.error(msg(), e)
    }
}