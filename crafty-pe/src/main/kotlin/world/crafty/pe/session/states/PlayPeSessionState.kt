package world.crafty.pe.session.states

import io.vertx.core.eventbus.Message
import kotlinx.coroutines.experimental.launch
import org.joml.Vector3f
import world.crafty.common.utils.logger
import world.crafty.common.vertx.*
import world.crafty.pe.PeLocation
import world.crafty.pe.PeNetworkSession
import world.crafty.pe.entity.PeEntity
import world.crafty.pe.entity.PePlayerEntity
import world.crafty.pe.jwt.payloads.LoginExtraData
import world.crafty.pe.jwt.payloads.PeClientData
import world.crafty.pe.proto.PeItem
import world.crafty.pe.proto.PePacket
import world.crafty.pe.proto.PeSkin
import world.crafty.pe.proto.packets.client.*
import world.crafty.pe.proto.packets.mixed.ChatPePacket
import world.crafty.pe.proto.packets.mixed.ChatType
import world.crafty.pe.proto.packets.mixed.MoveMode
import world.crafty.pe.proto.packets.mixed.SetPlayerLocPePacket
import world.crafty.pe.proto.packets.server.*
import world.crafty.pe.raknet.RakMessageReliability
import world.crafty.pe.session.PeSessionState
import world.crafty.pe.toPe
import world.crafty.pe.world.toPePacket
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.CraftyPacket
import world.crafty.proto.CraftySkin
import world.crafty.proto.MinecraftPlatform
import world.crafty.proto.packets.client.*
import world.crafty.proto.packets.server.*
import kotlin.reflect.KClass

private val log = logger<PlayPeSessionState>()
class PlayPeSessionState(
        session: PeNetworkSession,
        val loginExtraData: LoginExtraData,
        val loginClientData: PeClientData,
        val craftySkin: CraftySkin
) : PeSessionState(session) {
    private val eb = session.vertx.eventBus()
    
    private var craftyPlayerId = 0
    
    private var ownEntityId: Int = 0
    private val loadedEntities = mutableMapOf<Int, PeEntity>()
    private var clientLoc = PeLocation(0f, 0f, 0f)
    
    suspend override fun handle(packet: PePacket) {
        when(packet) {
            is ResourcePackClientResponsePePacket -> {
                val startGame: suspend () -> Unit = {
                    val username = loginExtraData.displayName
                    val joinRequest = JoinRequestCraftyPacket(username, true, false, MinecraftPlatform.PE, craftySkin)
                    val joinResponse = vxm<JoinResponseCraftyPacket> { eb.send("${session.worldServer}:join", joinRequest, it) }
                    if (!joinResponse.accepted) {
                        throw IllegalStateException("CraftyServer denied our join request :(") // TODO: disconnect here
                    }

                    val prespawn = joinResponse.prespawn!!
                    ownEntityId = prespawn.entityId
                    clientLoc = PeLocation(prespawn.spawnLocation)
                    session.queueSend(StartGamePePacket(
                            entityId = ownEntityId.toLong(),
                            runtimeEntityId = 0,
                            spawn = PeLocation(prespawn.spawnLocation),
                            seed = 12345,
                            dimension = 0,
                            generator = 1,
                            gamemode = prespawn.gamemode,
                            difficulty = 0,
                            x = 0,
                            y = 50,
                            z = 0,
                            achievementsDisabled = true,
                            dayCycleStopTime = 6000,
                            eduEdition = false,
                            rainLevel = 0f,
                            lightningLevel = 0f,
                            enableCommands = false,
                            resourcePackRequired = false,
                            levelId = "1m0AAMIFIgA=",
                            worldName = "test"
                    ))

                    craftyPlayerId = joinResponse.playerId!!

                    session.queueSend(SetChunkRadiusPePacket(5))
                    session.queueSend(SetAttributesPePacket(0, PlayerAttribute.defaults))
                }

                when(packet.status) {
                    ResourcePackClientStatus.REQUEST_DATA -> {
                        startGame()
                    }
                    ResourcePackClientStatus.PLAYER_READY -> {
                        startGame()
                    }
                    else -> {
                        throw NotImplementedError("unhandled resource pack status ${packet.status}")
                    }
                }
            }
            is ChunkRadiusRequestPePacket -> {
                val chunkRadius = Math.min(5, packet.desiredChunkRadius)
                session.queueSend(SetChunkRadiusPePacket(chunkRadius))

                registerCraftyConsumers()

                val cache = session.server.getWorldCache(session.worldServer)
                val response = sendCraftyAsync<ChunksRadiusResponseCraftyPacket>(ChunksRadiusRequestCraftyPacket(clientLoc.positionVec3(), 0, chunkRadius)).body()
                response.chunkColumns.forEach { setColumn ->
                    val chunkPacket = when (setColumn.cacheStrategy) {
                        ChunkCacheStategy.PER_WORLD -> cache.getOrComputeChunkPacket(setColumn, CraftyChunkColumn::toPePacket)
                        ChunkCacheStategy.PER_PLAYER -> setColumn.chunkColumn.toPePacket()
                    }
                    session.send(chunkPacket, RakMessageReliability.RELIABLE_ORDERED)
                }
                log.debug { "sent pe ${response.chunkColumns.size} chunks" }

                sendCrafty(ReadyToSpawnCraftyPacket())
            }
            is PlayerActionPePacket -> {
                val action = packet.action
                val craftyAction = when(action) {
                    PePlayerAction.START_BREAK -> return
                    PePlayerAction.ABORT_BREAK -> return
                    PePlayerAction.STOP_BREAK -> return
                    PePlayerAction.UNKNOWN_3 -> return
                    PePlayerAction.UNKNOWN_4 -> return
                    PePlayerAction.RELEASE_ITEM -> PlayerAction.DROP_ITEM
                    PePlayerAction.STOP_SLEEPING -> PlayerAction.LEAVE_BED
                    PePlayerAction.RESPAWN -> return
                    PePlayerAction.JUMP -> return
                    PePlayerAction.START_SPRINT -> PlayerAction.START_SPRINT
                    PePlayerAction.STOP_SPRINT -> PlayerAction.STOP_SPRINT
                    PePlayerAction.START_SNEAK -> PlayerAction.START_SNEAK
                    PePlayerAction.STOP_SNEAK -> PlayerAction.STOP_SNEAK
                    PePlayerAction.START_DIMENSION_CHANGE -> return
                    PePlayerAction.ABORT_DIMENSION_CHANGE -> return
                    PePlayerAction.START_GLIDE -> return
                    PePlayerAction.STOP_GLIDE -> return
                }
                sendCrafty(PlayerActionCraftyPacket(craftyAction))
            }
            is SetPlayerLocPePacket -> {
                if(packet.mode != MoveMode.INTERPOLATE) {
                    log.warn { "Received PE move of mode ${packet.mode} from ${loginExtraData.displayName}" }
                    return
                }
                val newLoc = packet.loc.copy(y = packet.loc.y - PePlayerEntity.eyeHeight)

                val posChanged = newLoc.x != clientLoc.x || newLoc.y != clientLoc.y || newLoc.z != clientLoc.z
                val lookChanged = newLoc.bodyYaw != clientLoc.bodyYaw || newLoc.headYaw != clientLoc.headYaw || newLoc.headPitch != clientLoc.headPitch

                val craftyPacket: CraftyPacket = if(!posChanged && !lookChanged) { return }
                else if(posChanged && !lookChanged) {
                    SetPlayerPosCraftyPacket(Vector3f(newLoc.x, newLoc.y, newLoc.z), packet.onGround)
                } else if(posChanged && lookChanged) {
                    SetPlayerPosAndLookCraftyPacket(newLoc.toLocation(), packet.onGround)
                } else {
                    SetPlayerLookCraftyPacket(headPitch = newLoc.headPitch, headYaw = newLoc.headYaw, bodyYaw = newLoc.bodyYaw)
                }

                sendCrafty(craftyPacket)

                clientLoc = newLoc
            }
            is ChatPePacket -> {
                sendCrafty(ChatFromClientCraftyPacket(packet.text))
            }
            else -> {
                log.warn { "Unhandled pe message ${packet.javaClass.simpleName}" }
            }
        }
    }
    
    fun registerCraftyConsumers() {
        craftyConsumer(PingCraftyPacket::class) { packet ->
            sendCrafty(PongCraftyPacket(packet.id))
        }
        
        craftyConsumer(ChatMessageCraftyPacket::class) { packet ->
            session.queueSend(ChatPePacket(ChatType.CHAT, "", packet.text))
        }

        craftyConsumer(AddPlayerCraftyPacket::class) { packet ->
            val entity = PePlayerEntity(packet.entityId.toLong())
            loadedEntities[packet.entityId] = entity

            if(packet.craftyId == craftyPlayerId)
                return@craftyConsumer

            val metadata = entity.metaFromCrafty(session.server.metaTranslatorRegistry, packet.meta)

            session.queueSend(PlayerListItemPePacket(
                    action = PlayerListAction.ADD,
                    items = listOf(
                            PlayerListPeAdd(
                                    uuid = packet.uuid,
                                    entityId = entity.id,
                                    name = packet.username,
                                    skin = PeSkin.fromCrafty(packet.skin)
                            )
                    )
            ))
            session.queueSend(AddPlayerPePacket(
                    uuid = packet.uuid,
                    username = packet.username,
                    entityId = entity.id,
                    runtimeEntityId = entity.id,
                    x = packet.location.x,
                    y = packet.location.y,
                    z = packet.location.z,
                    speedX = 0f,
                    speedY = 0f,
                    speedZ = 0f,
                    headPitch = packet.location.headPitch.toDegrees(),
                    headYaw = packet.location.bodyYaw.toDegrees(),
                    bodyYaw = packet.location.bodyYaw.toDegrees(),
                    itemInHand = PeItem(0, 0, 0, null),
                    metadata = metadata
            ))
            session.queueSend(PlayerListItemPePacket(PlayerListAction.REMOVE, listOf(PlayerListPeRemove(packet.uuid))))
        }
        
        craftyConsumer(RemovePlayerCraftyPacket::class) { packet ->
            session.queueSend(RemoveEntityPePacket(packet.entityId.toLong()))
        }

        craftyConsumer(PatchEntityCraftyPacket::class) { packet ->
            val entity = loadedEntities[packet.entityId] ?: return@craftyConsumer
            val meta = entity.metaFromCrafty(session.server.metaTranslatorRegistry, packet.values)
            session.queueSend(EntityMetadataPePacket(entity.id, meta))
        }

        craftyConsumer(SetEntityLocationCraftyPacket::class) { packet ->
            if(packet.entityId == ownEntityId) return@craftyConsumer

            val entity = loadedEntities[packet.entityId] ?: return@craftyConsumer

            session.queueSend(entity.getSetLocationPacket(packet.location.toPe(), packet.onGround))
        }

        craftyConsumer(SpawnSelfCraftyPacket::class) {
            session.queueSend(PlayerStatusPePacket(PlayerStatus.SPAWN))
        }
        
        craftyConsumer(DisconnectPlayerCraftyPacket::class) {
            session.disconnect(it.message)
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
            log.debug { "Error while sending packet ${packet::class.simpleName} to crafty for player ${loginExtraData.displayName}" }
            session.disconnect()
        }
    }

    suspend fun <TReply> sendCraftyAsync(packet: CraftyPacket) : Message<TReply> {
        try {
            return eb.typedSendAsync<TReply>("p:s:$craftyPlayerId", packet)
        } catch(e: Exception) {
            log.debug { "Error while sending packet ${packet::class.simpleName} to crafty for player ${loginExtraData.displayName}" }
            session.disconnect()
            throw e
        }
    }

    suspend override fun onDisconnect(message: String) {
        log.info { "Disconnected PE ${loginExtraData.displayName} ($message)" }
        
        try {
            eb.send("${session.worldServer}:quit", QuitCraftyPacket(craftyPlayerId))
        } catch(e: Exception) {
            log.warn("Failed to notify PE player ${loginExtraData.displayName} quit, did the backend server die?", e)
        }
    }
}