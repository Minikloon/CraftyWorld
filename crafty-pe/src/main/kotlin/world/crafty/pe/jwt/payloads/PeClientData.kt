package world.crafty.pe.jwt.payloads

import world.crafty.pe.peSkinToPng

class PeClientData(
        val ADRole: Int,
        val clientRandomId: Long,
        val currentInputMode: Int,
        val defaultInputMode: Int,
        val deviceModel: String,
        val deviceOS: Int,
        val gameVersion: String,
        val guiScale: Int,
        val serverAddress: String,
        val skinData: ByteArray,
        val skinId: String,
        val tenantId: String,
        val UIProfile: Int
) {
    val skinPng by lazy {
        peSkinToPng(skinData)
    }
}