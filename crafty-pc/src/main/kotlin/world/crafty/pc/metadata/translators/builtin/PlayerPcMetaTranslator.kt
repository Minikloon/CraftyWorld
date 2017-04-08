package world.crafty.pc.metadata.translators.builtin

import world.crafty.common.utils.firstByte
import world.crafty.pc.metadata.*
import world.crafty.pc.metadata.translators.MetaNotImplementedYet
import world.crafty.pc.metadata.translators.PcCraftyMetaTranslator
import world.crafty.pc.metadata.translators.PcCraftyMetaTranslatorGroup
import world.crafty.proto.metadata.MetaValue
import world.crafty.proto.metadata.builtin.PlayerMeta
import java.util.*

object PlayerPcMetaTranslator : PcCraftyMetaTranslatorGroup {
    override val translators = mapOf(
            PlayerMeta.ADDITIONAL_HEARTS to object: PcCraftyMetaTranslator {
                override fun fromCrafty(entity: PcEntity, meta: MetaValue) = mapOf(
                        11 to MetadataEntry(MetadataType.FLOAT, meta.value as Float)
                )
            },
            PlayerMeta.SCORE to MetaNotImplementedYet,
            PlayerMeta.DISPLAYED_SKIN_PARTS to MetaNotImplementedYet,
            PlayerMeta.MAIN_HAND to MetaNotImplementedYet,
            PlayerMeta.CROUCHED to object: PcCraftyMetaTranslator {
                override fun fromCrafty(entity: PcEntity, meta: MetaValue): Map<Int, MetadataEntry>? {                    
                    val prevValue = entity.metaCache.computeIfAbsent(0) {
                        BitSet(8)
                    } as BitSet
                    
                    prevValue[1] = meta.value as Boolean
                    
                    return mapOf(
                            0 to MetadataEntry(MetadataType.BYTE, prevValue.firstByte())
                    )
                }
            },
            PlayerMeta.SPRINTING to MetaNotImplementedYet,
            PlayerMeta.ELYTRA_FLY to MetaNotImplementedYet
    )
}