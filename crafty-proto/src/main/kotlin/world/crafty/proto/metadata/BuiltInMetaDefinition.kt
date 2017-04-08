package world.crafty.proto.metadata

import world.crafty.proto.metadata.builtin.*

private val builtIn = listOf(
        EntityMeta,
        LivingMeta,
        PlayerMeta
)

private var registered = false
private val lock = Any()

fun MetaFieldRegistry.registerBuiltInMetaDefinitions() {
    synchronized(lock) {
        if(registered) return
        registered = true
    }
    
    builtIn.forEach {
        it.getFields().forEach {
            registerField(it)
        }
    }
}