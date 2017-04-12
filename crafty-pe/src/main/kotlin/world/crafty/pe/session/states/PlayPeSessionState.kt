package world.crafty.pe.session.states

import org.joml.Vector3f
import world.crafty.common.utils.logger
import world.crafty.common.vertx.typedConsumer
import world.crafty.common.vertx.typedSend
import world.crafty.common.vertx.vxm
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
                    if (!joinResponse.accepted)
                        throw IllegalStateException("CraftyServer denied our join request :(")

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

                    eb.consumer<ChatMessageCraftyPacket>("p:c:$craftyPlayerId:chat") { // TODO: move this somewhere more sensible
                        val text = it.body().text
                        session.queueSend(ChatPePacket(ChatType.CHAT, "", text))
                    }

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

                eb.typedConsumer("p:c:$craftyPlayerId", AddPlayerCraftyPacket::class) {
                    val packet = it.body()

                    val entity = PePlayerEntity(packet.entityId.toLong())
                    loadedEntities[packet.entityId] = entity

                    if(packet.craftyId == craftyPlayerId)
                        return@typedConsumer

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

                eb.typedConsumer("p:c:$craftyPlayerId", PatchEntityCraftyPacket::class) {
                    val packet = it.body()
                    val entity = loadedEntities[packet.entityId] ?: return@typedConsumer
                    val meta = entity.metaFromCrafty(session.server.metaTranslatorRegistry, packet.values)
                    session.queueSend(EntityMetadataPePacket(entity.id, meta))
                }

                eb.typedConsumer("p:c:$craftyPlayerId", SetEntityLocationCraftyPacket::class) {
                    val packet = it.body()
                    if(packet.entityId == ownEntityId) return@typedConsumer

                    val entity = loadedEntities[packet.entityId] ?: return@typedConsumer

                    session.queueSend(entity.getSetLocationPacket(packet.location.toPe(), packet.onGround))
                }

                eb.typedConsumer("p:c:$craftyPlayerId", SpawnSelfCraftyPacket::class) {
                    session.queueSend(PlayerStatusPePacket(PlayerStatus.SPAWN))
                }

                val cache = session.server.getWorldCache(session.worldServer)
                eb.typedSend<ChunksRadiusResponseCraftyPacket>("p:s:$craftyPlayerId", ChunksRadiusRequestCraftyPacket(clientLoc.positionVec3(), 0, chunkRadius)) {
                    val response = it.result().body()
                    response.chunkColumns.forEach { setColumn ->
                        val chunkPacket = when(setColumn.cacheStrategy) {
                            ChunkCacheStategy.PER_WORLD -> cache.getOrComputeChunkPacket(setColumn, CraftyChunkColumn::toPePacket)
                            ChunkCacheStategy.PER_PLAYER -> setColumn.chunkColumn.toPePacket()
                        }
                        session.send(chunkPacket, RakMessageReliability.RELIABLE_ORDERED)
                    }
                    log.debug { "sent pe ${response.chunkColumns.size} chunks" }

                    eb.typedSend("p:s:$craftyPlayerId", ReadyToSpawnCraftyPacket())
                }
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
                eb.typedSend("p:s:$craftyPlayerId", PlayerActionCraftyPacket(craftyAction))
            }
            is SetPlayerLocPePacket -> {
                if(packet.mode != MoveMode.INTERPOLATE) {
                    log.warn { "Received PE move of mode ${packet.mode} from ${loginExtraData?.displayName}" }
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

                eb.typedSend("p:s:$craftyPlayerId", craftyPacket)

                clientLoc = newLoc
            }
            is ChatPePacket -> {
                eb.typedSend("p:s:$craftyPlayerId", ChatFromClientCraftyPacket(packet.text))
            }
            else -> {
                log.warn { "Unhandled pe message ${packet.javaClass.simpleName}" }
            }
        }
    }
}