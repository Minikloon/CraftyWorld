package world.crafty.pe.raknet

val unconnectedBlabber: ByteArray = listOf(0x00, 0xff, 0xff, 0x00, 0xfe, 0xfe, 0xfe, 0xfe, 0xfd, 0xfd, 0xfd, 0xfd, 0x12, 0x34, 0x56, 0x78).map(Int::toByte).toByteArray()