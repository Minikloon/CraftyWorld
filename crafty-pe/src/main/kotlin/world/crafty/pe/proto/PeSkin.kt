package world.crafty.pe.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.pngToRawSkin
import world.crafty.proto.CraftySkin

class PeSkin(
        val name: String,
        val texture: ByteArray
) {
    object Codec : PeCodec<PeSkin> {
        override fun serialize(obj: PeSkin, stream: MinecraftOutputStream) {
            stream.writeUnsignedString(obj.name)
            stream.writeUnsignedVarInt(obj.texture.size)
            stream.write(obj.texture)
        }
        override fun deserialize(stream: MinecraftInputStream): PeSkin {
            return PeSkin(
                    name = stream.readUnsignedString(),
                    texture = stream.readByteArray(stream.readUnsignedVarInt())
            )
        }
    }
    
    companion object {
        fun fromCrafty(craftySkin: CraftySkin) : PeSkin {
            val texture = pngToRawSkin(craftySkin.pngBytes)
            return PeSkin("Standard_Custom", texture)
        }
    }
}