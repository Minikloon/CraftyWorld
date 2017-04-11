package world.crafty.pe.metadata.translators

import world.crafty.common.utils.getLogger
import world.crafty.common.utils.warn
import world.crafty.pe.metadata.MetadataEntry
import world.crafty.pe.entity.PeEntity
import world.crafty.proto.metadata.MetaField
import world.crafty.proto.metadata.MetaValue

interface PeCraftyMetaTranslator {
    fun fromCrafty(entity: PeEntity, meta: MetaValue) : Map<Int, MetadataEntry>?
}

object MetaNotSupportedOnPe : PeCraftyMetaTranslator {
    override fun fromCrafty(entity: PeEntity, meta: MetaValue): Map<Int, MetadataEntry>? {
        return null
    }
}

object MetaNotImplementedYet : PeCraftyMetaTranslator {
    var called = false
    override fun fromCrafty(entity: PeEntity, meta: MetaValue): Map<Int, MetadataEntry>? {
        if(!called) {
            getLogger("pe-meta").warn { "PE meta translator for crafty field ${meta.fieldId} isn't implemented yet! GET ON IT!" }
            called = true
        }
        return null
    }
}

class MetaTranslatorRegistry {
    private val translators = mutableMapOf<Int, Registration>()
    
    fun registerTranslator(field: MetaField, translator: PeCraftyMetaTranslator) {
        val registration = Registration(field, translator)
        val previous = translators.put(field.id, registration)
        if(previous != null)
            throw IllegalStateException("Dupe field id ${field.id} for pe meta translator '${field.name}' and ${previous.field.name}")
    }
    
    operator fun get(field: MetaField) : PeCraftyMetaTranslator? {
        return get(field.id)
    }
    
    operator fun get(fieldId: Int) : PeCraftyMetaTranslator? {
        return translators[fieldId]?.translator
    }
    
    private class Registration(
            val field: MetaField,
            val translator: PeCraftyMetaTranslator
    )
}