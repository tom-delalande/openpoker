package server

import app.logger
import domain.table.TableService
import domain.tournament.CashGameService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import server.models.AuthResponse
import server.models.HandEvent
import server.models.PlayerAction

private val json = Json

fun Route.authEndpoints(
    authRepository: AuthRepository,
    gameService: CashGameService,
    idGenerator: () -> Int = { UUID.randomUUID().hashCode() },
) {
    route("/auth") {
        post("/login") {
            val name = call.queryParameters["name"] ?: throw IllegalStateException()
            val playerId = idGenerator()
            val token = UUID.randomUUID()
            authRepository.saveToken(token, AuthRepository.PlayerInfo(playerId))
            gameService.createPlayer(playerId, name)
            call.respond(AuthResponse(token.toString(), playerId))
        }
    }
}

fun Route.gameEndpoints(gameService: CashGameService, authRepository: AuthRepository) {
    route("/game") {
        get("/player") {
            val token = call.request.queryParameters["token"]?.let { UUID.fromString(it) }
                ?: throw IllegalStateException()
            val player = authRepository.getPlayer(token) ?: throw IllegalStateException()
            val gamePlayer = gameService.getPlayer(player.playerId)
            call.respond(gamePlayer)
        }

        post("/join") {
            val token = call.request.queryParameters["token"]?.let { UUID.fromString(it) }
                ?: throw IllegalStateException()
            val playerId = authRepository.getPlayer(token)?.playerId ?: throw IllegalStateException()
            val player = gameService.getPlayer(playerId)
            val tableId = gameService.createOrJoin(player.id, player.name, player.stack)
            call.respond(tableId.toString())
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun Route.tableEndpoints(
    websockets: ConcurrentHashMap<UUID, MutableSharedFlow<HandEvent>>,
    authRepository: AuthRepository,
    tableService: TableService,
) {
    route("/table") {
        webSocket("/ws/table/{tableId}/token/{token}") {
            val sessionId = UUID.randomUUID()
            val tableId = call.parameters["tableId"]?.let { UUID.fromString(it) } ?: throw IllegalStateException()
            val token = call.parameters["token"]?.let { UUID.fromString(it) } ?: throw IllegalStateException()
            val messageResponseFlow = MutableSharedFlow<HandEvent>()
            val sharedFlow = messageResponseFlow.asSharedFlow()

            val player = authRepository.getPlayer(token) ?: return@webSocket
            websockets[sessionId] = messageResponseFlow

            tableService.addWebSocketConnection(player.playerId, tableId, sessionId, Instant.now())

            val job = launch {
                sharedFlow.collect { message ->
                    sendSerialized(message)
                }
            }

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        try {
                            val receivedText = frame.readText()
                            val playerAction = json.decodeFromString<PlayerAction>(receivedText)
                            tableService.receivePlayerActions(
                                sessionId,
                                player.playerId,
                                listOf(playerAction.toDomain(player.playerId))
                            )
                        } catch (exception: Exception) {
                            logger.error("WebSocket exception: ${exception.localizedMessage}", exception)
                        }
                    }
                }
            } finally {
                logger.info("Websocket has been closed. token[$token] tableId[$tableId]")
                tableService.removeWebSocketConnection(tableId, sessionId, Instant.now())
                job.cancel()
            }
        }
    }
}