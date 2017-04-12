package world.crafty.pc.session.states

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.joml.Vector3f
import world.crafty.common.Angle256
import world.crafty.common.Location
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.utils.logger
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
import world.crafty.pc.world.toPcPacket
import world.crafty.proto.CraftyChunkColumn
import world.crafty.proto.packets.client.*
import world.crafty.proto.packets.server.*
import world.crafty.skinpool.CraftySkinPoolServer
import world.crafty.skinpool.protocol.client.HashPollPoolPacket
import world.crafty.skinpool.protocol.server.HashPollReplyPoolPacket

private val log = logger<PlayPcSessionState>()
class PlayPcSessionState(
        session: PcNetworkSession,
        val craftyPlayerId: Int,
        val profile: MojangProfile,
        val prespawn: PreSpawnCraftyPacket
) : PcSessionState(session) {
    override val packetList = ServerBoundPcPlayPackets
    private val eb = session.server.vertx.eventBus()

    private lateinit var brand : String
    private var ownEntityId = 0
    private var location = Location(0f, 0f, 0f)
    private val loadedEntities = mutableMapOf<Int, PcEntity>()
    
    val server = session.server

    suspend override fun onStart() {
        eb.consumer<ChatMessageCraftyPacket>("p:c:$craftyPlayerId:chat") {
            val text = it.body().text
            session.send(ServerChatMessagePcPacket(McChat(text), ChatPosition.CHAT_BOX))
        }

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

        eb.typedSend<ChunksRadiusResponseCraftyPacket>("p:s:$craftyPlayerId", ChunksRadiusRequestCraftyPacket(prespawn.spawnLocation.positionVec3(), 0, 8)) {
            val response = it.result().body()
            val cache = server.getWorldCache(session.worldServer)
            response.chunkColumns.forEach { setColumn ->
                val chunkPacket = when(setColumn.cacheStrategy) {
                    ChunkCacheStategy.PER_WORLD -> cache.getOrComputeChunkPacket(setColumn, CraftyChunkColumn::toPcPacket)
                    ChunkCacheStategy.PER_PLAYER -> setColumn.chunkColumn.toPcPacket()
                }
                session.send(chunkPacket)
            }

            eb.typedConsumer("p:c:$craftyPlayerId", AddPlayerCraftyPacket::class) {
                val packet = it.body()

                if(packet.craftyId == craftyPlayerId)
                    return@typedConsumer

                launch(CurrentVertx) {
                    val skinReq = HashPollPoolPacket(packet.skin.fnvHashOfPng, needProfile = true)
                    val skinProfile = eb.typedSendAsync<HashPollReplyPoolPacket>(CraftySkinPoolServer.channelPrefix, skinReq).body()
                    val playerProps = if(skinProfile.textureProp == null) emptyList() else listOf(skinProfile.textureProp!!)

                    val entity = PcEntity(packet.entityId, packet.location)
                    loadedEntities[entity.id] = entity

                    if(packet.craftyId == craftyPlayerId)
                        return@launch

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
                    delay(1000)
                    session.send(PlayerListItemPcPacket(PlayerListAction.REMOVE, listOf(PlayerListPcRemove(packet.uuid))))
                }
            }

            eb.typedConsumer("p:c:$craftyPlayerId", PatchEntityCraftyPacket::class) {
                val packet = it.body()
                val entity = loadedEntities[packet.entityId] ?: return@typedConsumer
                val meta = entity.metaFromCrafty(server.metaTranslatorRegistry, packet.values)
                session.send(EntityMetadataPcPacket(packet.entityId, meta))
            }

            eb.typedConsumer("p:c:$craftyPlayerId", SetEntityLocationCraftyPacket::class) {
                val packet = it.body()
                if(packet.entityId == ownEntityId) return@typedConsumer

                val entity = loadedEntities[packet.entityId] ?: return@typedConsumer

                entity.getMovePacketsAndClear(packet.location, packet.onGround).forEach {
                    session.send(it)
                }
            }

            eb.typedConsumer("p:c:$craftyPlayerId", SpawnSelfCraftyPacket::class) {
                launch(CurrentVertx) {
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
                    delay(1000)
                    session.send(PlayerListItemPcPacket(
                            action = PlayerListAction.REMOVE,
                            items = listOf(PlayerListPcRemove(profile.uuid))
                    ))
                }
            }

            eb.typedSend("p:s:$craftyPlayerId", ReadyToSpawnCraftyPacket())
        }

        setPeriodic(1000) {
            session.send(ServerKeepAlivePcPacket(0))
        }
    }
    
    suspend override fun handle(packet: PcPacket) {
        when (packet) {
            is ClientSettingsPcPacket -> {
                log.info { "Client settings like locale ${packet.locale}" }
            }
            is ClientKeepAlivePcPacket -> {

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
                eb.typedSend("p:s:$craftyPlayerId", ChatFromClientCraftyPacket(packet.text))
            }
            is PlayerPositionPcPacket -> {
                val pos = Vector3f(packet.x.toFloat(), packet.y.toFloat(), packet.z.toFloat())
                eb.typedSend("p:s:$craftyPlayerId", SetPlayerPosCraftyPacket(pos, packet.onGround))
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
                eb.typedSend("p:s:$craftyPlayerId", SetPlayerPosAndLookCraftyPacket(pos, packet.onGround))
            }
            is PlayerLookPcPacket -> {
                val headPitch = Angle256.fromDegrees(packet.pitch)
                val headYaw = Angle256.fromDegrees(packet.yaw)
                val bodyYaw = headYaw
                eb.typedSend("p:s:$craftyPlayerId", SetPlayerLookCraftyPacket(headPitch, headYaw, bodyYaw))
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
                eb.typedSend("p:s:$craftyPlayerId", PlayerActionCraftyPacket(craftyAction))
            }
            is PlayerTeleportConfirmPcPacket -> {
                log.info { "teleport confirm ${packet.confirmId}" }
            }
            else -> {
                log.error { "Unhandled Play packet ${packet.javaClass.simpleName}" }
            }
        }
    }

    suspend override fun onStop() {
        
    }
}