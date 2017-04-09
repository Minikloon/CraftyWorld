package world.crafty.pc.metadata.translators

import world.crafty.common.utils.getLogger
import world.crafty.common.utils.warn
import world.crafty.pc.metadata.MetadataEntry
import world.crafty.pc.metadata.PcEntity
import world.crafty.proto.metadata.MetaField
import world.crafty.proto.metadata.MetaValue

interface PcCraftyMetaTranslator {
    fun fromCrafty(entity: PcEntity, meta: MetaValue) : Map<Int, MetadataEntry>?
}

object MetaNotSupportedOnPc : PcCraftyMetaTranslator {
    override fun fromCrafty(entity: PcEntity, meta: MetaValue): Map<Int, MetadataEntry>? {
        return null
    }
}

object MetaNotImplementedYet : PcCraftyMetaTranslator {
    var called = false
    override fun fromCrafty(entity: PcEntity, meta: MetaValue): Map<Int, MetadataEntry>? {
        if(!called) {
            getLogger("pc-metadata").warn { "PC meta translator for crafty field ${meta.fieldId} isn't implemented yet! GET ON IT!" }
            called = true
        }
        return null
    }
}

class MetaTranslatorRegistry {
    private val translators = mutableMapOf<Int, Registration>()
    
    fun registerTranslator(field: MetaField, translator: PcCraftyMetaTranslator) {
        val registration = Registration(field, translator)
        val previous = translators.put(field.id, registration)
        if(previous != null)
            throw IllegalStateException("Dupe field id ${field.id} for pc meta translator '${field.name}' and ${previous.field.name}")
    }
    
    operator fun get(field: MetaField) : PcCraftyMetaTranslator? {
        return get(field.id)
    }
    
    operator fun get(fieldId: Int) : PcCraftyMetaTranslator? {
        return translators[fieldId]?.translator
    }
    
    private class Registration(
            val field: MetaField,
            val translator: PcCraftyMetaTranslator
    )
}