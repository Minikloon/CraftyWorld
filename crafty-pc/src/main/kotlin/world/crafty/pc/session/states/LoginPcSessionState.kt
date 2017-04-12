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
import world.crafty.proto.packets.server.JoinResponseCraftyPacket
import world.crafty.skinpool.CraftySkinPoolServer
import world.crafty.skinpool.protocol.client.HashPollPoolPacket
import world.crafty.skinpool.protocol.client.SaveSkinPoolPacket
import world.crafty.skinpool.protocol.server.HashPollReplyPoolPacket
import java.util.*
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
    
    suspend override fun onStart() {
        session.send(EncryptionRequestPcPacket("${session.sessionId}", session.server.x509PubKey, verifyToken))
    }

    suspend override fun handle(packet: PcPacket) {
        when(packet) {
            is EncryptionResponsePcPacket -> {
                val serverDecipher = session.server.decipher

                if(! Arrays.equals(serverDecipher.doFinal(packet.verifyToken), verifyToken)) {
                    logger(this).warn { "Session ${session.address} sent the wrong verification token" }
                    session.close()
                    return
                }
                val sharedSecret = SecretKeySpec(serverDecipher.doFinal(packet.sharedSecret), "AES")

                session.encryptionPass = MinecraftPcEncryptionPass(sharedSecret)
                logger(this).info { "Now encrypting with ${session.address}" }
                session.switchState(CompressionLoginStage(session, sharedSecret, username))
            }
            else -> {
                unexpectedPacket(packet)
            }
        }
    }
}

class CompressionLoginStage(
        session: PcNetworkSession,
        val sharedSecret: SecretKeySpec,
        val username: String
) : LoginStage(session) {
    suspend override fun onStart() {
        val pass = MinecraftCompressionPass()
        session.send(SetCompressionPcPacket(pass.threshold))
        session.compressionPass = pass
        session.switchState(MojangCheckLoginStage(session, sharedSecret, username))
    }

    suspend override fun handle(packet: PcPacket) {
        unexpectedPacket(packet)
    }
}

class MojangCheckLoginStage(
        session: PcNetworkSession,
        val sharedSecret: SecretKeySpec,
        val username: String
) : LoginStage(session) {
    suspend override fun onStart() {
        val serverId = MojangClient.getServerIdHash(session.sessionId, sharedSecret, session.server.x509PubKey)
        val profile = session.server.mojang.checkHasJoinedAsync(username, serverId)
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
    suspend override fun onStart() {
        val textureProp = profile.properties.first { it.name == "textures" } // TODO: cache pc skins in pool for faster login
        val texturePropValue = Base64.getDecoder().decode(textureProp.value)
        val textureJson = JsonObject(texturePropValue.toString(Charsets.UTF_8)).getJsonObject("textures")
        val skinJson = textureJson.getJsonObject("SKIN")
        val isSkinSlim = skinJson.getJsonObject("metadata") != null
        val skinUrl = skinJson.getString("url")
        
        val webClient = WebClient.create(session.server.vertx)
        val getSkinResponse = vxHttp { webClient.getAbs(skinUrl).send(it) }
        if(getSkinResponse.statusCode() != HttpResponseStatus.OK.code()) {
            throw IllegalStateException("Couldn't download pc skin for ${profile.name} at $skinUrl")
        }
        
        val skinPngBytes = getSkinResponse.body().bytes
        val skinHash = hashFnv1a64(skinPngBytes)
        val skin = CraftySkin(skinHash, skinPngBytes)

        val eb = session.server.vertx.eventBus()
        val skinPollReply = eb.typedSendAsync<HashPollReplyPoolPacket>(CraftySkinPoolServer.channelPrefix, HashPollPoolPacket(skinHash, false)).body()
        if(! skinPollReply.hasProfile) {
            eb.typedSend(CraftySkinPoolServer.channelPrefix, SaveSkinPoolPacket(skinHash, isSkinSlim, skinPngBytes))
        }

        val worldServer = session.worldServer
        val craftyResponse = vxm<JoinResponseCraftyPacket> { eb.send("$worldServer:join", JoinRequestCraftyPacket(profile.name, true, false, MinecraftPlatform.PC, skin), it) }
        if(!craftyResponse.accepted) {
            logger(this).info { "Crafty refused ${profile.name} from joining" }
            session.close()
            return
        }
        
        session.send(LoginSuccessPcPacket(profile.uuid, profile.name))
        
        session.switchState(PlayPcSessionState(session, craftyResponse.playerId!!, profile, craftyResponse.prespawn!!))
    }

    suspend override fun handle(packet: PcPacket) {
        unexpectedPacket(packet)
    }
}