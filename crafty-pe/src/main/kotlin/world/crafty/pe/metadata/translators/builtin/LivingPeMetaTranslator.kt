package world.crafty.pe.metadata.translators.builtin

import world.crafty.pe.metadata.translators.MetaNotImplementedYet
import world.crafty.pe.metadata.translators.PeCraftyMetaTranslatorGroup
import world.crafty.proto.metadata.builtin.LivingMeta

object LivingPeMetaTranslator : PeCraftyMetaTranslatorGroup {
    override val translators = mapOf(
            LivingMeta.HAND_STATE to MetaNotImplementedYet,
            LivingMeta.HEALTH to MetaNotImplementedYet,
            LivingMeta.POTION_EFFECT_COLOR to MetaNotImplementedYet,
            LivingMeta.POTION_EFFECT_AMBIENT to MetaNotImplementedYet,
            LivingMeta.ARROWS_PENETRATED to MetaNotImplementedYet
    )
}