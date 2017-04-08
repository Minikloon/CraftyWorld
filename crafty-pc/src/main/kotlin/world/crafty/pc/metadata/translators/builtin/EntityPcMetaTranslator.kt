package world.crafty.pc.metadata.translators.builtin

import world.crafty.pc.metadata.translators.MetaNotImplementedYet
import world.crafty.pc.metadata.translators.PcCraftyMetaTranslatorGroup
import world.crafty.proto.metadata.builtin.EntityMeta

object EntityPcMetaTranslator : PcCraftyMetaTranslatorGroup {
    override val translators = mapOf(
            EntityMeta.ON_FIRE to MetaNotImplementedYet,
            EntityMeta.INVISIBLE to MetaNotImplementedYet,
            EntityMeta.GLOWING to MetaNotImplementedYet,
            EntityMeta.CUSTOM_NAME to MetaNotImplementedYet,
            EntityMeta.SILENT to MetaNotImplementedYet,
            EntityMeta.NO_GRAVITY to MetaNotImplementedYet
    )
}