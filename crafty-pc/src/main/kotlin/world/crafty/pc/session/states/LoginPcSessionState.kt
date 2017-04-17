package world.crafty.pc.session.states

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import world.crafty.common.utils.hashFnv1a64
import world.crafty.common.utils.logger
import world.crafty.common.vertx.typedSend
import world.crafty.common.vertx.typedSendAsync
import world.crafty.common.vertx.vxHttp
import world.crafty.common.vertx.vxm
import world.crafty.mojang.MojangClient
import world.crafty.mojang.MojangProfile
import world.crafty.pc.PcNetworkSession
import world.crafty.pc.generateVerifyToken
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.proto.ServerBoundPcLoginPackets
import world.crafty.pc.proto.packets.client.EncryptionResponsePcPacket
import world.crafty.pc.proto.packets.client.LoginStartPcPacket
import world.crafty.pc.proto.packets.server.EncryptionRequestPcPacket
import world.crafty.pc.proto.packets.server.LoginSuccessPcPacket
import world.crafty.pc.proto.packets.server.SetCompressionPcPacket
import world.crafty.pc.session.PcSessionState
import world.crafty.pc.session.pass.MinecraftCompressionPass
import world.crafty.pc.session.pass.MinecraftPcEncryptionPass
import world.crafty.proto.CraftySkin
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.packets.client.JoinRequestCraftyPacket
import world.crafty.proto.packets.client.QuitCraftyPacket
import world.crafty.proto.packets.server.JoinResponseCraftyPacket
import world.crafty.skinpool.CraftySkinPoolServer
import world.crafty.skinpool.protocol.client.HashPollPoolPacket
import world.crafty.skinpool.protocol.client.SaveSkinPoolPacket
import world.crafty.skinpool.protocol.server.HashPollReplyPoolPacket
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.spec.SecretKeySpec

abstract class LoginStage(session: PcNetworkSession) : PcSessionState(session) {
    override val packetList = ServerBoundPcLoginPackets
}

class FirstPacketLoginStage(session: PcNetworkSession) : LoginStage(session) {
    suspend override fun handle(packet: PcPacket) {
        when (packet) {
            is LoginStartPcPacket -> {
                val username = packet.username
                logger(this).info { "Login start from $username" }
                session.switchState(EncryptionLoginStage(session, username))
            }
            else -> {
                unexpectedPacket(packet)
            }
        }
    }
}

class EncryptionLoginStage(
        session: PcNetworkSession,
        val username: String
) : LoginStage(session) {
    val verifyToken = generateVerifyToken()
    val sessionId = sessionIdCounter.getAndIncrement()
    
    suspend override fun onStart() {
        session.send(EncryptionRequestPcPacket("$sessionId", session.connServer.x509PubKey, verifyToken))
    }

    suspend override fun handle(packet: PcPacket) {
        when(packet) {
            is EncryptionResponsePcPacket -> {
                val serverDecipher = session.connServer.decipher

                if(! Arrays.equals(serverDecipher.doFinal(packet.verifyToken), verifyToken)) {
                    logger(this).warn { "Session ${session.address} sent the wrong verification token" }
                    session.disconnect("Handshake error")
                    return
                }
                val sharedSecret = SecretKeySpec(serverDecipher.doFinal(packet.sharedSecret), "AES")

                session.encryptionPass = MinecraftPcEncryptionPass(sharedSecret)
                logger(this).info { "Now encrypting with ${session.address}" }
                session.switchState(CompressionLoginStage(session, sharedSecret, sessionId, username))
            }
            else -> {
                unexpectedPacket(packet)
            }
        }
    }
    
    companion object {
        val sessionIdCounter = AtomicInteger()
    }
}

class CompressionLoginStage(
        session: PcNetworkSession,
        val sharedSecret: SecretKeySpec,
        val sessionId: Int,
        val username: String
) : LoginStage(session) {
    suspend override fun onStart() {
        val pass = MinecraftCompressionPass()
        session.send(SetCompressionPcPacket(pass.threshold))
        session.compressionPass = pass
        session.switchState(MojangCheckLoginStage(session, sharedSecret, sessionId, username))
    }

    suspend override fun handle(packet: PcPacket) {
        unexpectedPacket(packet)
    }
}

class MojangCheckLoginStage(
        session: PcNetworkSession,
        val sharedSecret: SecretKeySpec,
        val sessionId: Int,
        val username: String
) : LoginStage(session) {
    suspend override fun onStart() {
        val serverId = MojangClient.getServerIdHash(sessionId, sharedSecret, session.connServer.x509PubKey)
        val profile = session.connServer.mojang.checkHasJoinedAsync(username, serverId)
        session.switchState(CraftyLoginStage(session, profile))
    }

    suspend override fun handle(packet: PcPacket) {
        unexpectedPacket(packet)
    }
}

class CraftyLoginStage(
        session: PcNetworkSession,
        val profile: MojangProfile
) : LoginStage(session) {
    private var craftyResponse: JoinResponseCraftyPacket? = null
    
    suspend override fun onStart() {
        val textureProp = profile.properties.first { it.name == "textures" } // TODO: cache pc skins in pool for faster login
        val texturePropValue = Base64.getDecoder().decode(textureProp.value)
        val textureJson = JsonObject(texturePropValue.toString(Charsets.UTF_8)).getJsonObject("textures")
        val skinJson = textureJson.getJsonObject("SKIN")
        val isSkinSlim = skinJson.getJsonObject("metadata") != null
        val skinUrl = skinJson.getString("url")
        
        val webClient = WebClient.create(session.connServer.vertx)
        val getSkinResponse = vxHttp { webClient.getAbs(skinUrl).send(it) }
        if(getSkinResponse.statusCode() != HttpResponseStatus.OK.code()) {
            throw IllegalStateException("Couldn't download pc skin for ${profile.name} at $skinUrl")
        }
        
        val skinPngBytes = getSkinResponse.body().bytes
        val skinHash = hashFnv1a64(skinPngBytes)
        val skin = CraftySkin(skinHash, skinPngBytes)

        val eb = session.connServer.vertx.eventBus()
        val skinPollReply = eb.typedSendAsync<HashPollReplyPoolPacket>(CraftySkinPoolServer.channelPrefix, HashPollPoolPacket(skinHash, false)).body()
        if(! skinPollReply.hasProfile) {
            eb.typedSend(CraftySkinPoolServer.channelPrefix, SaveSkinPoolPacket(skinHash, isSkinSlim, skinPngBytes))
        }

        val worldServer = session.worldServer
        val joinResponse = vxm<JoinResponseCraftyPacket> { eb.send("$worldServer:join", JoinRequestCraftyPacket(profile.name, true, false, MinecraftPlatform.PC, skin), it) }
        if(!joinResponse.accepted) {
            logger(this).info { "Crafty refused ${profile.name} from joining" }
            session.disconnect("Connection refused")
            return
        }
        craftyResponse = joinResponse
        
        session.send(LoginSuccessPcPacket(profile.uuid, profile.name))
        
        session.switchState(PlayPcSessionState(session, joinResponse.playerId!!, profile, joinResponse.prespawn!!))
    }

    suspend override fun handle(packet: PcPacket) {
        unexpectedPacket(packet)
    }

    suspend override fun onDisconnect(message: String) {
        val joinResponse = craftyResponse ?: return
        val eb = session.connServer.vertx.eventBus()
        eb.send("${session.worldServer}:quit", QuitCraftyPacket(joinResponse.playerId!!))
    }
}