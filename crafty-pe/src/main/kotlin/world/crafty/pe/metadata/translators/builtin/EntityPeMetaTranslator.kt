package world.crafty.pe.metadata.translators.builtin

import world.crafty.pe.metadata.translators.MetaNotImplementedYet
import world.crafty.pe.metadata.translators.PeCraftyMetaTranslatorGroup
import world.crafty.proto.metadata.builtin.EntityMeta

object EntityPeMetaTranslator : PeCraftyMetaTranslatorGroup {
    override val translators = mapOf(
            EntityMeta.ON_FIRE to MetaNotImplementedYet,
            EntityMeta.INVISIBLE to MetaNotImplementedYet,
            EntityMeta.GLOWING to MetaNotImplementedYet,
            EntityMeta.CUSTOM_NAME to MetaNotImplementedYet,
            EntityMeta.SILENT to MetaNotImplementedYet,
            EntityMeta.NO_GRAVITY to MetaNotImplementedYet
    )
}