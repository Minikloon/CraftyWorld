package club.kazza.kazzacraft.nbt.tags

class NbtString(name: String?, val value: String) : NbtValueTag(name) {
    override val tagType: NbtTagType
        get() = NbtTagType.STRING

    override fun deepClone(): NbtTag {
        return NbtString(name, value)
    }

    override fun prettyValueStr(): String {
        return "\"$value\""
    }
}