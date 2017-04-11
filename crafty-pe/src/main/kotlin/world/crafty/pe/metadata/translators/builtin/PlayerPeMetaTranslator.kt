package world.crafty.pe.metadata.translators.builtin

import world.crafty.common.utils.firstLong
import world.crafty.pe.metadata.MetadataEntry
import world.crafty.pe.metadata.MetadataType
import world.crafty.pe.entity.PeEntity
import world.crafty.pe.metadata.translators.MetaNotImplementedYet
import world.crafty.pe.metadata.translators.PeCraftyMetaTranslator
import world.crafty.pe.metadata.translators.PeCraftyMetaTranslatorGroup
import world.crafty.proto.metadata.MetaValue
import world.crafty.proto.metadata.builtin.PlayerMeta
import java.util.*

object PlayerPeMetaTranslator : PeCraftyMetaTranslatorGroup {
    override val translators = mapOf(
            PlayerMeta.ADDITIONAL_HEARTS to MetaNotImplementedYet,
            PlayerMeta.SCORE to MetaNotImplementedYet,
            PlayerMeta.DISPLAYED_SKIN_PARTS to MetaNotImplementedYet,
            PlayerMeta.MAIN_HAND to MetaNotImplementedYet,
            PlayerMeta.CROUCHED to object: PeCraftyMetaTranslator {
                override fun fromCrafty(entity: PeEntity, meta: MetaValue): Map<Int, MetadataEntry>? {
                    val bitset = entity.metaCache.computeIfAbsent(0) {
                        BitSet(64)
                    } as BitSet
                    bitset[1] = meta.value as Boolean
                    
                    return mapOf(
                            0 to MetadataEntry(MetadataType.LONG, bitset.firstLong())
                    )
                }
            },
            PlayerMeta.SPRINTING to MetaNotImplementedYet,
            PlayerMeta.ELYTRA_FLY to MetaNotImplementedYet
    )
}