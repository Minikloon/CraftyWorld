package world.crafty.pe.metadata

import world.crafty.pe.metadata.translators.MetaTranslatorRegistry
import world.crafty.proto.metadata.MetaValue

class PeEntity(val id: Long) {
    val metaCache = mutableMapOf<Int, Any>()

    fun metaFromCrafty(translators: MetaTranslatorRegistry, meta: List<MetaValue>) : PeMetadataMap {
        val map = PeMetadataMap()
        meta.forEach {
            val translator = translators[it.fieldId]
                    ?: throw IllegalStateException("No pc meta translator for crafty field ${it.fieldId}")
            val mappings = translator.fromCrafty(this, it)
            mappings?.forEach { fieldId, translated ->
                map[fieldId] = translated
            }
        }
        return map
    }
}