package world.crafty.proto.metadata

import world.crafty.common.serialization.McCodec

abstract class MetaDefinition {    
    abstract fun getFields() : Collection<MetaField>
}

class MetaField(
        val id: Int,
        val name: String,
        val codec: MetaCodec
)

typealias MetaCodec = McCodec<Any?>