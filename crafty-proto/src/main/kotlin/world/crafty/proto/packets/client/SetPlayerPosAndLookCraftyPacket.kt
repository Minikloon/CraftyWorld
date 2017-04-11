package world.crafty.proto.packets.client

import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.proto.CraftyPacket

class SetPlayerPosAndLookCraftyPacket(
        val loc: Location,
        val onGround: Boolean
) : CraftyPacket() {
    override val codec = Codec
    object Codec : CraftyPacketCodec() {
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is SetPlayerPosAndLookCraftyPacket) throw IllegalArgumentException()
            stream.writeLocation(obj.loc)
            stream.writeBoolean(obj.onGround)
        }
        override fun deserialize(stream: MinecraftInputStream): CraftyPacket {
            return SetPlayerPosAndLookCraftyPacket(
                    loc = stream.readLocation(),
                    onGround = stream.readBoolean()
            )
        }
    }
}