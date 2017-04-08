package world.crafty.server.utils

import world.crafty.proto.CraftyPacket
import world.crafty.server.CraftyPlayer

fun Collection<CraftyPacket>.broadcast(players: Collection<CraftyPlayer>) {
    players.forEach { 
        it.send(this)
    }
}