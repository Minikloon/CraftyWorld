package world.crafty.pc.metadata

import world.crafty.pc.metadata.translators.MetaTranslatorRegistry
import world.crafty.proto.metadata.MetaValue

class PcEntity(val id: Long) {
    val metaCache = mutableMapOf<Int, Any>()

    fun metaFromCrafty(translators: MetaTranslatorRegistry, meta: List<MetaValue>) : PcMetadataMap {
        val map = PcMetadataMap()
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