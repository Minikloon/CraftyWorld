package world.crafty.proto.metadata.builtin

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.metadata.*
import java.util.*

class HorseMeta(
        state: HorseBitField = HorseBitField(),
        owner: UUID? = null
) : MetaTracker() {
    var state by netSync(this, STATE, state)
    val owner by netSync(this, OWNER, owner)

    companion object : MetaDefinition() {
        val STATE = MetaField(HORSE_META_SPACE+1, "state", HorseBitField.Codec)
        val OWNER = MetaField(HORSE_META_SPACE+2, "owner", NullableCodec(UuidCodec))

        override fun getFields(): Collection<MetaField> = listOf(
                STATE,
                OWNER
        )
    }
}

class HorseBitField(val bitfield: Byte) {
    constructor(
            tamed: Boolean = false,
            saddled: Boolean = false,
            hasBred: Boolean = false,
            eating: Boolean = false,
            rearing: Boolean = false,
            mouthOpen: Boolean = false
    ) : this((0
            or ifBit(tamed, 1)
            or ifBit(saddled, 2)
            or ifBit(hasBred, 3)
            or ifBit(eating, 4)
            or ifBit(rearing, 5)
            or ifBit(mouthOpen, 6)
            ).toByte())

    companion object {
        private fun ifBit(cond: Boolean, bit: Int) : Int {
            return if(cond) 1 shl bit else 0
        }
    }

    object Codec : MetaCodec {
        override fun serialize(obj: Any?, stream: MinecraftOutputStream) {
            if(obj !is HorseBitField) throw IllegalArgumentException()
            stream.writeByte(obj.bitfield)
        }
        override fun deserialize(stream: MinecraftInputStream): Any? {
            return HorseBitField(stream.readByte())
        }
    }
}