package world.crafty.pe.session.states

import world.crafty.common.utils.hashFnv1a64
import world.crafty.common.utils.logger
import world.crafty.common.vertx.typedSend
import world.crafty.pe.PeNetworkSession
import world.crafty.pe.generateBytes
import world.crafty.pe.isCertChainValid
import world.crafty.pe.isSlim
import world.crafty.pe.jwt.payloads.CertChainLink
import world.crafty.pe.jwt.payloads.PeClientData
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.packets.client.ConnectedPingPePacket
import world.crafty.pe.proto.packets.client.EntityFallPePacket
import world.crafty.pe.proto.packets.client.LoginPePacket
import world.crafty.pe.proto.packets.mixed.LevelSoundEventPePacket
import world.crafty.pe.proto.packets.server.PlayerStatus
import world.crafty.pe.proto.packets.server.PlayerStatusPePacket
import world.crafty.pe.proto.packets.server.ResourcePackTriggerPePacket
import world.crafty.pe.proto.packets.server.ServerHandshakePePacket
import world.crafty.pe.session.PeSessionState
import world.crafty.pe.session.pass.MinecraftPeEncryptionPass
import world.crafty.proto.CraftySkin
import world.crafty.skinpool.CraftySkinPoolServer
import world.crafty.skinpool.protocol.client.HashPollPoolPacket
import world.crafty.skinpool.protocol.client.SaveSkinPoolPacket
import world.crafty.skinpool.protocol.server.HashPollReplyPoolPacket
import java.security.MessageDigest
import java.time.Duration
import java.util.*
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec

private val log = logger<LoginPeSessionState>()
class LoginPeSessionState(session: PeNetworkSession) : ConnectedPeSessionState(session) {
    val server = session.server
    val eb = session.vertx.eventBus()
    
    suspend override fun handle(packet: PePacket) {
        when(packet) {
            is ConnectedPingPePacket -> {
                onPing(packet)
            }
            is LoginPePacket -> {
                val chain = packet.certChain

                val firstChainLink = chain[0].payload as CertChainLink
                if(firstChainLink.certificateAuthority && firstChainLink.idPubKey != PeNetworkSession.mojangPubKey) {
                    throw Exception("Login first chain link claims to be ca but doesn't hold the mojang pub key")
                }

                if(! isCertChainValid(chain))
                    throw NotImplementedError("Should do something about invalid cert chain")

                val lastClaim = chain.last().payload as CertChainLink
                val clientKey = lastClaim.idPubKey
                val loginExtraData = lastClaim.extraData!!

                val clientDataJwt = packet.clientData
                if(clientDataJwt.header.x5uKey != clientKey || !clientDataJwt.isSignatureValid) {
                    throw NotImplementedError("Should do something about invalid client data signature")
                }

                val clientData = clientDataJwt.payload as PeClientData
                val loginClientData = clientData
                val png = clientData.skinPng
                val skinHash = hashFnv1a64(png)
                val craftySkin = CraftySkin(skinHash, png)
                eb.typedSend<HashPollReplyPoolPacket>(CraftySkinPoolServer.channelPrefix, HashPollPoolPacket(skinHash, false)) {
                    if(it.failed()) {
                        it.cause().printStackTrace()
                        return@typedSend
                    }
                    val res = it.result()
                    if(! res.body().hasProfile) {
                        val slim = isSlim(clientData.skinData)
                        val skinPng = clientData.skinPng
                        eb.typedSend(CraftySkinPoolServer.channelPrefix, SaveSkinPoolPacket(skinHash, slim, skinPng))
                    }
                }

                if(server.supportsEncryption) { // TODO encrypt if xuid exists
                    val agreement = KeyAgreement.getInstance("ECDH")
                    agreement.init(server.keyPair.private)
                    agreement.doPhase(clientKey, true)
                    val sharedSecret = agreement.generateSecret()

                    val sessionToken = generateBytes(128)

                    val sha256 = MessageDigest.getInstance("SHA-256")
                    sha256.update(sessionToken)
                    val secretKeyBytes = sha256.digest(sharedSecret)
                    val iv = Arrays.copyOf(secretKeyBytes, 16)
                    val secretKey = SecretKeySpec(secretKeyBytes, "AES")
                    
                    session.encryptionPass = MinecraftPeEncryptionPass(secretKey, iv)
                    val handshake = ServerHandshakePePacket(server.keyPair.public, sessionToken)
                    session.queueSend(handshake)
                    // TODO
                }
                else {
                    session.queueSend(PlayerStatusPePacket(PlayerStatus.LOGIN_ACCEPTED))
                    session.queueSend(ResourcePackTriggerPePacket(false, listOf(), listOf()))
                    log.info { "Login from ${session.address} ${packet.protocolVersion} ${packet.edition}" }
                    session.switchState(PlayPeSessionState(session, loginExtraData, loginClientData, craftySkin))
                }
            }
            else -> {
                unexpectedPacket(packet)
            }
        }
    }

    override val pingTimeout: Duration = Duration.ofMillis(10_000)
}