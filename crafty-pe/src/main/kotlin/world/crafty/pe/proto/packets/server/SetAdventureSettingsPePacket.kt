package world.crafty.pe.proto.packets.server

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.proto.AdventureSettingsFlags
import world.crafty.pe.proto.PePacket

class SetAdventureSettingsPePacket(
        val settings: AdventureSettingsFlags,
        val permissionLevel: Int
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x37
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetAdventureSettingsPePacket) throw IllegalArgumentException()
            stream.writeUnsignedVarInt(obj.settings.bitField)
            stream.writeUnsignedVarInt(obj.permissionLevel)
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            return SetAdventureSettingsPePacket(
                    settings = AdventureSettingsFlags(stream.readUnsignedVarInt()),
                    permissionLevel = stream.readUnsignedVarInt()
            )
        }
    }
}