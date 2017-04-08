package world.crafty.proto.metadata.builtin

import world.crafty.proto.metadata.*

open class EntityMeta(
        onFire: Boolean = false,
        invisible: Boolean = false,
        glowing: Boolean = false,
        customName: String? = null,
        silent: Boolean = false,
        noGravity: Boolean = false
) : MetaTracker() {    
    var onFire by netSync(this, ON_FIRE, onFire)
    var invisible by netSync(this, INVISIBLE, invisible)
    var glowing by netSync(this, GLOWING, glowing)
    var customName by netSync(this, CUSTOM_NAME, customName)
    var silent by netSync(this, SILENT, silent)
    var noGravity by netSync(this, NO_GRAVITY, noGravity)
    
    companion object : MetaDefinition() {
        val ON_FIRE =  MetaField(ENTITY_META_SPACE + 1, "on fire", BooleanCodec)
        val INVISIBLE = MetaField(ENTITY_META_SPACE + 2, "invisible", BooleanCodec)
        val GLOWING = MetaField(ENTITY_META_SPACE + 3, "glowing", BooleanCodec)
        val CUSTOM_NAME = MetaField(ENTITY_META_SPACE + 4, "entity custom name", NullableCodec(StringCodec))
        val SILENT = MetaField(ENTITY_META_SPACE + 5, "silent", BooleanCodec)
        val NO_GRAVITY = MetaField(ENTITY_META_SPACE + 6, "no gravity", BooleanCodec)
        
        override fun getFields() = listOf(
                ON_FIRE,
                INVISIBLE,
                GLOWING,
                CUSTOM_NAME,
                SILENT,
                NO_GRAVITY
        )
    }
}