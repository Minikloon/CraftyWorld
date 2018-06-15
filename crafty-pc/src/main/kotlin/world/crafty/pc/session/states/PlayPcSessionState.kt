package world.crafty.pc.session.states

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.vertx.core.eventbus.Message
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.joml.Vector3f
import sun.misc.HexDumpEncoder
import world.crafty.common.Angle256
import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.utils.*
import world.crafty.common.vertx.CurrentVertx
import world.crafty.common.vertx.typedConsumer
import world.crafty.common.vertx.typedSend
import world.crafty.common.vertx.typedSendAsync
import world.crafty.mojang.MojangProfile
import world.crafty.pc.PcNetworkSession
import world.crafty.pc.entity.PcEntity
import world.crafty.pc.proto.McChat
import world.crafty.pc.proto.PcPacket
import world.crafty.pc.proto.ServerBoundPcPlayPackets
import world.crafty.pc.proto.packets.client.*
import world.crafty.pc.proto.packets.server.*
import world.crafty.pc.session.PcSessionState
import world.crafty.pc.world.PcChunk
import world.crafty.pc.world.PcChunkColumn
import world.crafty.pc.world.toPcPacket
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.CraftyPacket
import world.crafty.proto.packets.client.*
import world.crafty.proto.packets.server.*
import world.crafty.skinpool.CraftySkinPoolServer
import world.crafty.skinpool.protocol.client.HashPollPoolPacket
import world.crafty.skinpool.protocol.server.HashPollReplyPoolPacket
import java.nio.ByteOrder
import java.util.*
import kotlin.reflect.KClass

private val log = logger<PlayPcSessionState>()
class PlayPcSessionState(
        session: PcNetworkSession,
        val craftyPlayerId: Int,
        val profile: MojangProfile,
        val prespawn: PreSpawnCraftyPacket
) : PcSessionState(session) {
    override val packetList = ServerBoundPcPlayPackets
    private val eb = session.connServer.vertx.eventBus()

    private lateinit var brand : String
    private var ownEntityId = 0
    private var location = Location(0f, 0f, 0f)
    private val loadedEntities = mutableMapOf<Int, PcEntity>()
    
    val server = session.connServer

    suspend override fun onStart() {
        ownEntityId = prespawn.entityId
        location = prespawn.spawnLocation
        session.send(JoinGamePcPacket(
                eid = prespawn.entityId,
                gamemode = prespawn.gamemode,
                dimension = prespawn.dimension,
                difficulty = 0,
                maxPlayers = 0,
                levelType = "flat",
                reducedDebug = false
        ))
        session.send(ServerPluginMessagePcPacket("MC|Brand", server.encodedBrand))

        registerCraftyConsumers()

        session.send(SetPlayerAbilitiesPcPacket(0b1100, 0.1f, 0.05f))
        
        val chunkRadiusReq = ChunksRadiusRequestCraftyPacket(location.positionVec3(), 0, 4)
        val chunkRadiusRes = sendCraftyAsync<ChunksRadiusResponseCraftyPacket>(chunkRadiusReq).body()
        val cache = server.getWorldCache(session.worldServer)
        chunkRadiusRes.chunkColumns.forEach { setColumn ->
            val chunkPacket = when (setColumn.cacheStrategy) {
                ChunkCacheStategy.PER_WORLD -> cache.getOrComputeChunkPacket(setColumn, CraftyChunkColumn::toPcPacket)
                ChunkCacheStategy.PER_PLAYER -> setColumn.chunkColumn.toPcPacket()
            }
            session.send(chunkPacket)
        }

        sendCrafty(ReadyToSpawnCraftyPacket())
    }
    
    suspend override fun handle(packet: PcPacket) {
        when (packet) {
            is ClientSettingsPcPacket -> {
                log.info { "Client settings like locale ${packet.locale}" }
            }
            is ClientKeepAlivePcPacket -> {
                sendCrafty(PongCraftyPacket(packet.confirmId))
            }
            is ClientPluginMessagePcPacket -> {
                val channel = packet.channel
                val dataStream = MinecraftInputStream(packet.data)
                when(channel) {
                    "MC|Brand" -> {
                        brand = dataStream.readSignedString()
                    }
                    else -> {
                        log.warn { "Unhandled packet on channel $channel with ${packet.data.size} bytes" }
                    }
                }
            }
            is ClientChatMessagePcPacket -> {
                sendCrafty(ChatFromClientCraftyPacket(packet.text))
            }
            is PlayerPositionPcPacket -> {
                val pos = Vector3f(packet.x.toFloat(), packet.y.toFloat(), packet.z.toFloat())
                sendCrafty(SetPlayerPosCraftyPacket(pos, packet.onGround))
            }
            is PlayerPosAndLookPcPacket -> {
                val pos = Location(
                        x = packet.x.toFloat(),
                        y = packet.y.toFloat(),
                        z = packet.z.toFloat(),
                        bodyYaw = Angle256.fromDegrees(packet.yaw),
                        headYaw = Angle256.fromDegrees(packet.yaw),
                        headPitch = Angle256.fromDegrees(packet.pitch)
                )
                sendCrafty(SetPlayerPosAndLookCraftyPacket(pos, packet.onGround))
            }
            is PlayerLookPcPacket -> {
                val headPitch = Angle256.fromDegrees(packet.pitch)
                val headYaw = Angle256.fromDegrees(packet.yaw)
                val bodyYaw = headYaw
                sendCrafty(SetPlayerLookCraftyPacket(headPitch, headYaw, bodyYaw))
            }
            is EntityActionPcPacket -> {
                val craftyAction = when(packet.action) {
                    PcEntityAction.START_SNEAKING -> PlayerAction.START_SNEAK
                    PcEntityAction.STOP_SNEAKING -> PlayerAction.STOP_SNEAK
                    PcEntityAction.LEAVE_BED -> PlayerAction.LEAVE_BED
                    PcEntityAction.START_SPRINTING -> PlayerAction.START_SPRINT
                    PcEntityAction.STOP_SPRINTING -> PlayerAction.STOP_SNEAK
                    PcEntityAction.START_HORSE_JUMP -> return
                    PcEntityAction.STOP_HORSE_JUMP -> return
                    PcEntityAction.OPEN_HORSE_INVENTORY -> return
                    PcEntityAction.START_ELYTRA_FLY -> return
                }
                sendCrafty(PlayerActionCraftyPacket(craftyAction))
            }
            is PlayerTeleportConfirmPcPacket -> {
                log.info { "teleport confirm ${packet.confirmId}" }
            }
            is SwingArmPcPacket -> {
                sendCrafty(PlayerActionCraftyPacket(PlayerAction.SWING_ARM))
            }
            else -> {
                log.error { "Unhandled Play packet ${packet.javaClass.simpleName}" }
            }
        }
    }
    
    fun registerCraftyConsumers() {
        craftyConsumer(PingCraftyPacket::class) {
            session.send(ServerKeepAlivePcPacket(it.id))
        }
        
        craftyConsumer(ChatMessageCraftyPacket::class) {
            val text = it.text
            session.send(ServerChatMessagePcPacket(McChat(text), ChatPosition.CHAT_BOX))
        }

        craftyConsumer(AddPlayerCraftyPacket::class) { packet ->
            if(packet.craftyId == craftyPlayerId)
                return@craftyConsumer

            val skinReq = HashPollPoolPacket(packet.skin.fnvHashOfPng, needProfile = true)
            val skinProfile = eb.typedSendAsync<HashPollReplyPoolPacket>(CraftySkinPoolServer.channelPrefix, skinReq).body()
            val playerProps = if(skinProfile.textureProp == null) emptyList() else listOf(skinProfile.textureProp!!)

            val entity = PcEntity(packet.entityId, packet.location)
            loadedEntities[entity.id] = entity

            if(packet.craftyId == craftyPlayerId)
                return@craftyConsumer

            val metadata = entity.metaFromCrafty(server.metaTranslatorRegistry, packet.meta)

            session.send(PlayerListItemPcPacket(
                    action = PlayerListAction.ADD,
                    items = listOf(
                            PlayerListPcAdd(
                                    uuid = packet.uuid,
                                    name = packet.username,
                                    properties = playerProps,
                                    gamemode = 0,
                                    ping = 30,
                                    displayName = null
                            )
                    )
            ))
            session.send(SpawnPlayerPcPacket(
                    entityId = packet.entityId,
                    uuid = packet.uuid,
                    location = packet.location,
                    metadata = metadata
            ))
            session.send(SetEntityHeadLookPcPacket(
                    entityId = packet.entityId,
                    headYaw = packet.location.headYaw
            ))
        }
        
        craftyConsumer(RemovePlayerCraftyPacket::class) { packet ->
            loadedEntities.remove(packet.entityId)
            session.send(DestroyEntitiesPcPacket(listOf(packet.entityId)))
        }

        craftyConsumer(PatchEntityCraftyPacket::class) { packet ->
            val entity = loadedEntities[packet.entityId] ?: return@craftyConsumer
            val meta = entity.metaFromCrafty(server.metaTranslatorRegistry, packet.values)
            session.send(EntityMetadataPcPacket(packet.entityId, meta))
        }

        craftyConsumer(SetEntityLocationCraftyPacket::class) { packet ->
            if(packet.entityId == ownEntityId) return@craftyConsumer

            val entity = loadedEntities[packet.entityId] ?: return@craftyConsumer

            entity.getMovePacketsAndClear(packet.location, packet.onGround).forEach {
                session.send(it)
            }
        }

        craftyConsumer(SpawnSelfCraftyPacket::class) {
            session.send(PlayerListItemPcPacket(
                    action = PlayerListAction.ADD,
                    items = listOf(
                            PlayerListPcAdd(
                                    uuid = profile.uuid,
                                    name = profile.name,
                                    properties = profile.properties,
                                    gamemode = 0,
                                    ping = 30,
                                    displayName = null
                            )
                    )
            ))
            session.send(TeleportPlayerPcPacket(location, relativeFlags = 0, confirmId = 1))

            session.send(ScoreboardObjectivePcPacket("test", ScoreboardObjectiveAction.CREATE, "Hello!", 0))
            session.send(DisplayScoreboardPcPacket(ScoreboardPosition.SIDEBAR, "test"))
            session.send(UpdateScorePcPacket("Test line", UpdateScoreAction.UPSERT, "test", 0))
        }
        
        craftyConsumer(DisconnectPlayerCraftyPacket::class) {
            session.disconnect(it.message)
        }
        
        craftyConsumer(PlayerAnimationCraftyPacket::class) {
            val anim = when(it.animation) {
                Animation.SWING_ARM -> PcAnimation.SWING_MAIN_ARM
                else -> throw NotImplementedError("Missing animation mapping for crafty ${it.animation}")
            }
            
            session.send(AnimationPcPacket(
                    entityId = it.entityId,
                    animation = anim
            ))
        }

        craftyConsumer(AddEntityCraftyPacket::class) {
            val pcEntity = PcEntity(it.entityId, it.location)
            loadedEntities[it.entityId] = pcEntity
            val metadata = pcEntity.metaFromCrafty(server.metaTranslatorRegistry, it.meta)
            val uuid = UUID.randomUUID()
            println("spawn entity with id ${it.entityId} for ${profile.name}")
            /*session.send(SpawnMobPcPacket(
                    it.entityId,
                    uuid,
                    29, // 29 = horse, bat = 3
                    it.location.x.toDouble(),
                    it.location.y.toDouble(),
                    it.location.z.toDouble(),
                    it.location.bodyYaw,
                    Angle256.zero,
                    it.location.headPitch,
                    0, 0, 0,
                    metadata
            ))*/
            session.send(SpawnObjectPcPacket(
                    it.entityId,
                    uuid,
                    78, // 78 = armor stand,
                    it.location,
                    0,
                    0, 0, 0
            ))

            if(profile.name == "Minikloon") {
                delay(100)
                val vehicleId = it.entityId
                server.sessions.values.forEach { otherSession ->
                    if(otherSession == this.session) {
                        otherSession.send(SetPassengersPcPacket(vehicleId, listOf(ownEntityId)))
                        println("sent set passenger to ${(otherSession.state as PlayPcSessionState).profile.name} $ownEntityId")
                    } else {
                        otherSession.send(SetPassengersPcPacket(vehicleId, listOf(ownEntityId)))
                        println("sent set passenger to ${(otherSession.state as PlayPcSessionState).profile.name} $ownEntityId")
                    }
                }

                setPeriodic(50) {
                    pcEntity.getMovePacketsAndClear(pcEntity.loc.add(0f, 0f, 0.08f), false).forEach { packet ->
                        server.sessions.values.forEach { otherSession ->
                            otherSession.send(packet)
                        }
                    }
                }
            }
        }
    }
    

    fun <T: Any> craftyMessageConsumer(clazz: KClass<T>, onReceive: suspend (packet: Message<T>) -> Unit) {
        vertxTypedConsumer("p:c:$craftyPlayerId", clazz) {
            launch(CurrentVertx) {
                onReceive(it)
            }
        }
    }
    
    fun <T: Any> craftyConsumer(clazz: KClass<T>, onReceive: suspend (packet: T) -> Unit) {
        vertxTypedConsumer("p:c:$craftyPlayerId", clazz) {
            launch(CurrentVertx) {
                onReceive(it.body())
            }
        }
    }

    fun sendCrafty(packet: CraftyPacket) {
        try {
            eb.typedSend("p:s:$craftyPlayerId", packet)
        } catch(e: Exception) {
            log.debug { "Error while sending packet ${packet::class.simpleName} to crafty for player ${profile.name}" }
            session.disconnect()
        }
    }

    suspend fun <TReply> sendCraftyAsync(packet: CraftyPacket) : Message<TReply> {
        try {
            return eb.typedSendAsync<TReply>("p:s:$craftyPlayerId", packet)
        } catch(e: Exception) {
            log.debug { "Error while sending packet ${packet::class.simpleName} to crafty for player ${profile.name}" }
            session.disconnect()
            throw e
        }
    }

    suspend override fun onDisconnect(message: String) {
        session.send(DisconnectPcPacket(McChat(message)))
        log.info { "Disconnected PC player ${profile.name} ($message)" }
        
        try {
            eb.send("${session.worldServer}:quit", QuitCraftyPacket(craftyPlayerId))
        } catch(e: Exception) {
            log.warn("Failed to notify PC player ${profile.name} quit, did the backend server die?", e)
        }
    }
}