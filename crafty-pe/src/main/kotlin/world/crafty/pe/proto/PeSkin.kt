package world.crafty.pe.proto

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pe.isSlim
import world.crafty.pe.pngToRawSkin
import world.crafty.proto.CraftySkin

class PeSkin(
        val type: String,
        val texture: ByteArray
) {
    object Codec : PeCodec<PeSkin> {
        override fun serialize(obj: PeSkin, stream: MinecraftOutputStream) {
            stream.writeUnsignedString(obj.type)
            stream.writeUnsignedVarInt(obj.texture.size)
            stream.write(obj.texture)
        }
        override fun deserialize(stream: MinecraftInputStream): PeSkin {
            return PeSkin(
                    type = stream.readUnsignedString(),
                    texture = stream.readByteArray(stream.readUnsignedVarInt())
            )
        }
    }
    
    companion object {
        fun fromCrafty(craftySkin: CraftySkin) : PeSkin {
            val texture = pngToRawSkin(craftySkin.pngBytes)
            val type = if(isSlim(texture)) "slim" else ""
            return PeSkin(type, texture)
        }
    }
}