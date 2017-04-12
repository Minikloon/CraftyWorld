package world.crafty.pe.proto.packets.mixed

import org.joml.Vector3fc
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.PePacket

class LevelSoundEventPePacket(
        val event: SoundEvent,
        val position: Vector3fc,
        val blockId: Int,
        val entityType: Int,
        val babyMob: Boolean,
        val global: Boolean
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x1a
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is LevelSoundEventPePacket) throw IllegalArgumentException()
            stream.writeByte(obj.event.ordinal)
            stream.writeVector3fLe(obj.position)
            stream.writeUnsignedVarInt(obj.blockId)
            stream.writeUnsignedVarInt(obj.entityType)
            stream.writeBoolean(obj.babyMob)
            stream.writeBoolean(obj.global)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return LevelSoundEventPePacket(
                    event = SoundEvent.values()[stream.readUnsignedByte()],
                    position = stream.readVector3fLe(),
                    blockId = stream.readUnsignedVarInt(),
                    entityType = stream.readUnsignedVarInt(),
                    babyMob = stream.readBoolean(),
                    global = stream.readBoolean()
            )
        }
    }
}

enum class SoundEvent {
    ITEM_USE_ON,
    HIT,
    STEP,
    JUMP,
    BREAK,
    PLACE,
    HEAVY_STEP,
    GALLOP,
    FALL,
    AMBIENT,
    AMBIENT_BABY,
    AMBIENT_IN_WATER,
    BREATHE,
    DEATH,
    DEATH_IN_WATER,
    DEATH_TO_ZOMBIE,
    HURT,
    HURT_IN_WATER,
    MAD,
    BOOST,
    BOW,
    SQUISH_BIG,
    SQUISH_SMALL,
    FALL_BIG,
    FALL_SMALL,
    SPLASH,
    FIZZ,
    FLAP,
    SWIM,
    DRINK,
    EAT,
    TAKE_OFF,
    SHAKE,
    PLOP,
    LAND,
    SADDLE,
    ARMOR,
    ADD_CHEST,
    THROW,
    ATTACK,
    ATTACK_NO_DAMAGE,
    WARN,
    SHEAR,
    MILK,
    THUNDER,
    EXPLODE,
    FIRE,
    IGNITE,
    FUSE,
    STARE,
    SPAWN,
    SHOOT,
    BREAK_BLOCK,
    REMEDY,
    UNFECT,
    LEVEL_UP,
    BOW_HIT,
    BULLET_HIT,
    EXTINGUISH_FIRE,
    ITEM_FIZZ,
    CHEST_OPEN,
    CHEST_CLOSED,
    POWER_ON,
    POWER_OFF,
    ATTACH,
    DETACH,
    DENY,
    TRIPOD,
    POP,
    DROP_SLOT,
    NOTE,
    THORNS,
    PISTON_IN,
    PISTON_OUT,
    PORTAL,
    WATER,
    LAVA_POP,
    LAVA,
    BURP,
    BUCKET_FILL_WATER,
    BUCKET_FILL_LAVA,
    BUCKET_EMPTY_WATER,
    BUCKET_EMPTY_LAVA,
    GUARDIAN_FLOP,
    GUARDIAN_CURSE,
    MOB_WARNING,
    MOB_WARNING_BABY,
    TELEPORT,
    SHULKER_OPEN,
    SHULKER_CLOSE,
    HAGGLE,
    HAGGLE_YES,
    HAGGLE_NO,
    HAGGLE_IDLE
}