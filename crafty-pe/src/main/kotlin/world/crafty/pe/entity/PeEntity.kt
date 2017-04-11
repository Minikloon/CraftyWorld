package world.crafty.pe.entity

import world.crafty.pe.PeLocation
import world.crafty.pe.metadata.PeMetadataMap
import world.crafty.pe.metadata.translators.MetaTranslatorRegistry
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.packets.server.SetEntityLocPePacket
import world.crafty.proto.metadata.MetaValue

open class PeEntity(val id: Long) {
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
    
    open fun getSetLocationPacket(loc: PeLocation, onGround: Boolean) : PePacket {
        return SetEntityLocPePacket(id, loc)
    }
}