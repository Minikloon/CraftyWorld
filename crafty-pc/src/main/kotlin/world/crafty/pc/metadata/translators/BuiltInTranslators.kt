package world.crafty.pc.metadata.translators

import world.crafty.pc.metadata.translators.builtin.EntityPcMetaTranslator
import world.crafty.pc.metadata.translators.builtin.LivingPcMetaTranslator
import world.crafty.pc.metadata.translators.builtin.PlayerPcMetaTranslator
import world.crafty.proto.metadata.MetaField

interface PcCraftyMetaTranslatorGroup {
    val translators: Map<MetaField, PcCraftyMetaTranslator>
}

private val builtIn = listOf(
        EntityPcMetaTranslator,
        LivingPcMetaTranslator,
        PlayerPcMetaTranslator
)

fun MetaTranslatorRegistry.registerBuiltInPcTranslators() {
    builtIn.forEach {
        it.translators.forEach { field, value ->
            registerTranslator(field, value)
        }
    }
}