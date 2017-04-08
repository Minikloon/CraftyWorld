package world.crafty.proto.metadata.builtin

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.metadata.*

open class PlayerMeta(
        additionalHearts: Float = 0f,
        score: Int = 0,
        displayedSkinParts: DisplayedSkinPartsBitField = DisplayedSkinPartsBitField(
                cape = true,
                jacket = true,
                leftArm = true,
                rightArm = true,
                leftLeg = true,
                rightLeg = true,
                hat = true
        ),
        mainHand: Int = 0,
        crouched: Boolean = false,
        sprinting: Boolean = false,
        elytraFly: Boolean = false
) : MetaTracker() {    
    var additionalHearts by netSync(this, ADDITIONAL_HEARTS, additionalHearts)
    var score by netSync(this, SCORE, score)
    var displayedSkinParts by netSync(this, DISPLAYED_SKIN_PARTS, displayedSkinParts)
    var mainHand by netSync(this, MAIN_HAND, mainHand)
    var crouched by netSync(this, CROUCHED, crouched)
    var sprinting by netSync(this, SPRINTING, sprinting)
    var elytraFly by netSync(this, ELYTRA_FLY, elytraFly)
    
    companion object : MetaDefinition() {
        val ADDITIONAL_HEARTS = MetaField(PLAYER_META_SPACE+1, "additional hearts", FloatCodec)
        val SCORE = MetaField(PLAYER_META_SPACE+2, "score", IntCodec)
        val DISPLAYED_SKIN_PARTS = MetaField(PLAYER_META_SPACE+3, "displayed skin parts", DisplayedSkinPartsBitField.Codec)
        val MAIN_HAND = MetaField(PLAYER_META_SPACE+4, "main hand", IntCodec)
        val CROUCHED = MetaField(PLAYER_META_SPACE+5, "crouched", BooleanCodec)
        val SPRINTING = MetaField(PLAYER_META_SPACE+6, "sprinting", BooleanCodec)
        val ELYTRA_FLY = MetaField(PLAYER_META_SPACE+7, "elytra fly", BooleanCodec)
        
        override fun getFields() = listOf(
                ADDITIONAL_HEARTS,
                SCORE,
                DISPLAYED_SKIN_PARTS,
                MAIN_HAND,
                CROUCHED,
                SPRINTING,
                ELYTRA_FLY
        )
    }
}

class DisplayedSkinPartsBitField(val bitfield: Byte) {
    constructor(
            cape: Boolean = false,
            jacket: Boolean = false,
            leftArm: Boolean = false,
            rightArm: Boolean = false,
            leftLeg: Boolean = false,
            rightLeg: Boolean = false,
            hat: Boolean = false
    ) : this((0
            or ifBit(cape, 0)
            or ifBit(jacket, 1)
            or ifBit(leftArm, 2)
            or ifBit(rightArm, 3)
            or ifBit(leftLeg, 4)
            or ifBit(rightLeg, 5)
            or ifBit(hat, 6)
            ).toByte())
            
    val cape by lazy { bitfield.toInt() and 0b1 != 0 }
    val jacket by lazy { bitfield.toInt() and 0b10 != 0 }
    val leftArm by lazy { bitfield.toInt() and 0b100 != 0 }
    val rightArm by lazy { bitfield.toInt() and 0b1000 != 0 }
    val leftLeg by lazy { bitfield.toInt() and 0b1_0000 != 0 }
    val rightLeg by lazy { bitfield.toInt() and 0b10_0000 != 0 }
    val hat by lazy { bitfield.toInt() and 0b100_0000 != 0 }
    
    companion object {
        private fun ifBit(cond: Boolean, bit: Int) : Int {
            return if(cond) 1 shl bit else 0
        }
    }
    
    object Codec : MetaCodec {
        override fun serialize(obj: Any?, stream: MinecraftOutputStream) {
            if(obj !is DisplayedSkinPartsBitField) throw IllegalArgumentException()
            stream.writeByte(obj.bitfield)
        }
        override fun deserialize(stream: MinecraftInputStream): Any {
            return DisplayedSkinPartsBitField(stream.readByte())
        }
    }
}