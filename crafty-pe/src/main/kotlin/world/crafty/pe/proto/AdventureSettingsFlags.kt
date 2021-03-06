package world.crafty.pe.proto

class AdventureSettingsFlags(val bitField: Int) {
    val worldImmutable: Boolean
    val noPvp: Boolean
    val noPve: Boolean
    val passiveMobs: Boolean
    val autoJump: Boolean
    val allowFly: Boolean
    val noClip: Boolean
    val worldBuilder: Boolean
    

    init {
        worldImmutable = isBit1(bitField, 0)
        noPvp = isBit1(bitField, 1)
        noPve = isBit1(bitField, 2)
        passiveMobs = isBit1(bitField, 3)
        autoJump = isBit1(bitField, 5)
        allowFly = isBit1(bitField, 6)
        noClip = isBit1(bitField, 7)
        worldBuilder = isBit1(bitField, 8)
    }

    constructor(worldImmutable: Boolean = false,
                noPvp: Boolean = false, 
                noPve: Boolean = false, 
                passiveMobs: Boolean = false, 
                autoJump: Boolean = false, 
                allowFly: Boolean = false, 
                noClip: Boolean = false, 
                worldBuilder: Boolean = false
    ) : this(paramsToBitflag(
            worldImmutable, 
            noPvp, noPve, 
            passiveMobs, 
            autoJump,
            allowFly,
            noClip, 
            worldBuilder))

    companion object {
        private fun paramsToBitflag(
                worldImmutable: Boolean, 
                noPvp: Boolean, 
                noPve: Boolean, 
                passiveMobs: Boolean, 
                autoJump: Boolean, 
                allowFly: Boolean, 
                noClip: Boolean,
                worldBuilder: Boolean
        ) : Int {
            return  ifTrueAelse0(worldImmutable, 0x01) or
                    ifTrueAelse0(noPvp, 0x02) or
                    ifTrueAelse0(noPve, 0x04) or
                    ifTrueAelse0(passiveMobs, 0x08) or
                    ifTrueAelse0(autoJump, 0x20) or
                    ifTrueAelse0(allowFly, 0x40) or
                    ifTrueAelse0(noClip, 0x80) or 
                    ifTrueAelse0(worldBuilder, 0x81)
        }

        private fun isBit1(value: Int, indexFromRight: Int) : Boolean {
            val shifted = value ushr indexFromRight
            return (shifted and 0b1) == 1
        }

        private fun ifTrueAelse0(cond: Boolean, a: Int) : Int {
            return if(cond) a else 0
        }
    }
}