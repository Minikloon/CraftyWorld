package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PeCodec
import world.crafty.pe.proto.PePacket

enum class PlayerAttributeType(innerId: String, val minValue: Float, val maxValue: Float, val defaultValue: Float) {
    ABSORPTION("absorption", 0f, Float.MAX_VALUE, 0f),
    ATTACK_DAMAGE("attack_damage", 0f, Float.MAX_VALUE, 0f),
    KNOCKBACK_RESISTANCE("knockback_resistance", 0f, 1f, 0f),
    LUCK("luck", -1025f, 1024f, 0f),
    FALL_DAMAGE("fall_damage", 0f, Float.MAX_VALUE, 1f),
    HEALTH("health", 0f, 20f, 20f),
    MOVEMENT_SPEED("movement", 0f, Float.MAX_VALUE, 0.1f),
    FOLLOW_RANGE("follow_range", 0f, 2048f, 16f),
    SATURATION("player.saturation", 0f, 20f, 5f),
    EXHAUSTION("player.exhaustion", 0f, 5f, 0.41f),
    HUNGER("player.hunger", 0f, 20f, 20f),
    EXPERIENCE_LEVEL("player.level", 0f, 24791f, 0f),
    EXPERIENCE("player.experience", 0f, 1f, 0f),
    ;

    val id = "minecraft:$innerId"

    fun value(value: Float) : PlayerAttribute {
        return PlayerAttribute(minValue, maxValue, value, defaultValue, id)
    }

    companion object {
        val byId = values().associateBy { it.id }
    }
}
class PlayerAttribute(
        val minValue: Float,
        val maxValue: Float,
        val value: Float,
        val defaultValue: Float,
        val name: String
) {
    object Codec : PeCodec<PlayerAttribute> {
        override fun serialize(obj: PlayerAttribute, stream: MinecraftOutputStream) {
            stream.writeFloatLe(obj.minValue)
            stream.writeFloatLe(obj.maxValue)
            stream.writeFloatLe(obj.value)
            stream.writeFloatLe(obj.defaultValue)
            stream.writeUnsignedString(obj.name)
        }
        override fun deserialize(stream: MinecraftInputStream): PlayerAttribute {
            return PlayerAttribute(
                    minValue = stream.readFloat(),
                    maxValue = stream.readFloat(),
                    value = stream.readFloat(),
                    defaultValue = stream.readFloat(),
                    name = stream.readUnsignedString()
            )
        }
    }

    companion object {
        val defaults = PlayerAttributeType.values().map { it.value(it.defaultValue) }
    }
}
class SetAttributesPePacket(
        val entityId: Long,
        val attributes: List<PlayerAttribute>
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x1f
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetAttributesPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarLong(obj.entityId)
            stream.writeUnsignedVarInt(obj.attributes.size)
            obj.attributes.forEach { PlayerAttribute.Codec.serialize(it, stream) }
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetAttributesPePacket(
                    entityId = stream.readUnsignedVarLong(),
                    attributes = (1..stream.readUnsignedVarInt()).map { PlayerAttribute.Codec.deserialize(stream) }
            )
        }
    }
}