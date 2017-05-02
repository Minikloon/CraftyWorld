package world.crafty.pc.world

enum class Dimension(val id: Int) {
    OVERWORLD(0),
    NETHER(-1),
    END(1),
    ;
    
    companion object {
        val byId = values().associateBy { it.id }
    }
}