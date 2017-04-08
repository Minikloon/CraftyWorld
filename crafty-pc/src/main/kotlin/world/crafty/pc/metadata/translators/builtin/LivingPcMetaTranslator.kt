package world.crafty.pc.metadata.translators.builtin

import world.crafty.pc.metadata.translators.MetaNotImplementedYet
import world.crafty.pc.metadata.translators.PcCraftyMetaTranslatorGroup
import world.crafty.proto.metadata.builtin.LivingMeta

object LivingPcMetaTranslator : PcCraftyMetaTranslatorGroup {
    override val translators = mapOf(
            LivingMeta.HAND_STATE to MetaNotImplementedYet,
            LivingMeta.HEALTH to MetaNotImplementedYet,
            LivingMeta.POTION_EFFECT_COLOR to MetaNotImplementedYet,
            LivingMeta.POTION_EFFECT_AMBIENT to MetaNotImplementedYet,
            LivingMeta.ARROWS_PENETRATED to MetaNotImplementedYet
    )
}