package world.crafty.skinpool

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.Json
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import world.crafty.common.utils.sinceThen
import world.crafty.common.vertx.*
import world.crafty.mojang.MojangClient
import world.crafty.skinpool.protocol.client.HashPollPoolPacket
import world.crafty.skinpool.protocol.client.SaveSkinPoolPacket
import world.crafty.skinpool.protocol.registerVertxSkinPoolCodecs
import world.crafty.skinpool.protocol.server.HashPollReplyPoolPacket
import world.crafty.skinpool.protocol.server.SaveProfilePoolPacket
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO

class CraftySkinPoolServer private constructor(
        private val accounts: List<AccountCredentials>,
        private val hashProfileRepo: FileHashProfileRepo
) : AbstractVerticle() {    
    val uploaders = mutableListOf<SkinUploader>()
    val skinQueue: Queue<SaveSkinPoolPacket> = LinkedList()
    
    var requestsToMojangThisMinute = 0
    
    override fun start() {
        val eb = vertx.eventBus()

        Json.mapper.registerKotlinModule()
        registerVertxSkinPoolCodecs(eb)
        
        launch(CurrentVertx) start@ {
            accounts.map {
                val mojangClient = MojangClient(vertx)
                val authFuture = async(CurrentVertx) { mojangClient.authenticateAsync(it.username, it.password) }
                Triple(it, mojangClient, authFuture)
            }.forEach {
                val account = it.first
                try {
                    it.third.await()
                } catch(e: Exception) {
                    println("Failed to authenticate account with user ${account.username} & uuid ${account.uuid}")
                    e.printStackTrace()
                    return@forEach
                }
                uploaders.add(SkinUploader(account.uuid, it.second))
            }
            
            
            if(uploaders.isEmpty()) {
                println("No account is authenticated, can't upload new skins!")
            }
            else {
                println("${uploaders.size} accounts ready for upload, ${uploaders.size - accounts.size} failed")
            }
            
            eb.typedConsumer(channelPrefix, HashPollPoolPacket::class) {
                launch(CurrentVertx) {
                    val hash = it.body().hash
                    val textureProfile = hashProfileRepo[hash]

                    val sendProfile = if(it.body().needProfile) textureProfile else null
                    it.reply(HashPollReplyPoolPacket(hash, textureProfile != null, sendProfile))
                }
            }
            
            eb.typedConsumer(channelPrefix, SaveSkinPoolPacket::class) {
                val saveSkin = it.body()
                skinQueue.add(saveSkin)
                println("add saveskin to queue ${saveSkin.hash}")
            }
            
            eb.typedConsumer(channelPrefix, SaveProfilePoolPacket::class) {
                val body = it.body()
                println("saving ${body.hash} to file")
                hashProfileRepo[body.hash] = body.textureProperty
            }
        }
        
        vertx.setPeriodic(2_000) { processUploadQueue() }
        vertx.setPeriodic(60_000) { requestsToMojangThisMinute = 0 }
    }
    
    private fun processUploadQueue() {
        val availableUploaders = uploaders
                .filter { it.lastUpload.sinceThen() > SkinUploader.uploadRateLimit }
        
        val processCount = Math.min(availableUploaders.size, skinQueue.size)
        
        for(i in 0 until processCount) {
            val mojangUploader = availableUploaders[i]
            val skin = skinQueue.poll() ?: return
            println("process upload for ${skin.hash}")

            launch(CurrentVertx) {
                if(mojangUploader.lastUpload.sinceThen() < SkinUploader.uploadRateLimit) {
                    skinQueue.add(skin)
                    return@launch
                }
                
                val img = ImageIO.read(ByteArrayInputStream(skin.skinPng))
                val skinProfileProperty = mojangUploader.uploadAsync(img, skin.slim)
                println("uploaded ${skin.hash}")
                vertx.eventBus().typedPublish(channelPrefix, SaveProfilePoolPacket(skin.hash, skinProfileProperty))
            }
        }
    }
    
    companion object {
        val channelPrefix = "skinpool"
        
        private val accountsFile = "skinpool-accounts.json"
        private val profilesFile = "skinpool-profiles"
        
        fun startFromConsole() : CraftySkinPoolServer {
            Json.mapper.registerKotlinModule()
            
            val accountsPath = Paths.get(accountsFile)
            if(! Files.exists(accountsPath)) {
                val bundled = this::class.java.getResourceAsStream("/$accountsFile")
                Files.copy(bundled, accountsPath)
                println("Generated $accountsFile, fill it in and restart")
            }

            val profilesPath = Paths.get(profilesFile)
            if(! Files.exists(profilesPath))
                Files.createFile(profilesPath)
            val repo = FileHashProfileRepo(profilesPath)
            repo.loadFromFile(profilesPath)
            
            val accounts = Json.mapper.readValue(File(accountsFile), Array<AccountCredentials>::class.java).toList()
            
            return CraftySkinPoolServer(accounts, repo)
        }
    }
}