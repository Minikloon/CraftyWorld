package world.crafty.pc.world

import world.crafty.pc.world.World

data class BlockType(val id: Int, val data: Int)

data class Block(val world: World, val x: Int, val Y: Int, val z: Int, val type: BlockType)