package world.crafty.proto.metadata.builtin

import world.crafty.proto.metadata.*

open class LivingMeta(        
        handState: Int = 0,
        health: Float = 1f,
        potionEffectColor: Int = 0,
        potionEffectAmbient: Boolean = false,
        arrowsPenetrated: Int = 0
) : MetaTracker() {    
    var handState by netSync(this, HAND_STATE, handState)
    var health by netSync(this, HEALTH, health)
    var potionEffectColor by netSync(this, POTION_EFFECT_COLOR, potionEffectColor)
    var potionEffectAmbient by netSync(this, POTION_EFFECT_AMBIENT, potionEffectAmbient)
    var arrowsPenetrated by netSync(this, ARROWS_PENETRATED, arrowsPenetrated)

    companion object : MetaDefinition() {
        val HAND_STATE = MetaField(LIVING_META_SPACE+1, "hand state", IntCodec)
        val HEALTH = MetaField(LIVING_META_SPACE+2, "health", FloatCodec)
        val POTION_EFFECT_COLOR = MetaField(LIVING_META_SPACE+3, "potion effect color", IntCodec)
        val POTION_EFFECT_AMBIENT = MetaField(LIVING_META_SPACE+4, "potion effect ambient", BooleanCodec)
        val ARROWS_PENETRATED = MetaField(LIVING_META_SPACE+5, "arrows penetrated", IntCodec)
        
        override fun getFields() = listOf(
                HAND_STATE,
                HEALTH,
                POTION_EFFECT_COLOR,
                POTION_EFFECT_AMBIENT,
                ARROWS_PENETRATED
        )
    }
}