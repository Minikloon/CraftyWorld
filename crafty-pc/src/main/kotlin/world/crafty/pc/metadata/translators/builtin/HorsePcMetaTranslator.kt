package world.crafty.pc.metadata.translators.builtin

import world.crafty.pc.entity.PcEntity
import world.crafty.pc.metadata.MetadataEntry
import world.crafty.pc.metadata.MetadataType
import world.crafty.pc.metadata.translators.PcCraftyMetaTranslator
import world.crafty.pc.metadata.translators.PcCraftyMetaTranslatorGroup
import world.crafty.proto.metadata.MetaValue
import world.crafty.proto.metadata.builtin.HorseBitField
import world.crafty.proto.metadata.builtin.HorseMeta
import java.util.*

object HorsePcMetaTranslator : PcCraftyMetaTranslatorGroup {
    override val translators = mapOf(
            HorseMeta.STATE to object: PcCraftyMetaTranslator {
                override fun fromCrafty(entity: PcEntity, meta: MetaValue) = mapOf(
                        13 to MetadataEntry(MetadataType.BYTE, (meta.value as HorseBitField).bitfield)
                )
            },
            HorseMeta.OWNER to object: PcCraftyMetaTranslator {
                override fun fromCrafty(entity: PcEntity, meta: MetaValue) = mapOf(
                        14 to MetadataEntry(MetadataType.OPTIONAL_UUID, meta.value as UUID?)
                )
            }
    )
}