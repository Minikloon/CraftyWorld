package world.crafty.pe.metadata.translators

import world.crafty.pe.metadata.translators.builtin.EntityPeMetaTranslator
import world.crafty.pe.metadata.translators.builtin.LivingPeMetaTranslator
import world.crafty.pe.metadata.translators.builtin.PlayerPeMetaTranslator
import world.crafty.proto.metadata.MetaField

interface PeCraftyMetaTranslatorGroup {
    val translators: Map<MetaField, PeCraftyMetaTranslator>
}

private val builtIn = listOf(
        EntityPeMetaTranslator,
        LivingPeMetaTranslator,
        PlayerPeMetaTranslator
)

fun MetaTranslatorRegistry.registerBuiltInPeTranslators() {
    builtIn.forEach {
        it.translators.forEach { field, value ->
            registerTranslator(field, value)
        }
    }
}