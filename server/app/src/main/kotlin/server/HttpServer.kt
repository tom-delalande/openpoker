package server

import app.logger
import domain.table.TableService
import domain.tournament.CashGameService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
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

fun Route.authEndpoints(authRepository: AuthRepository, idGenerator: () -> Int = { Random.nextInt() }) {
    route("/auth") {
        post("/login/{name}") {
            val name = call.parameters["name"] ?: throw IllegalStateException()
            val playerId = idGenerator()
            val token = UUID.randomUUID().toString()
            authRepository.saveToken(token, playerId, name)
            call.respond(AuthResponse(token, playerId))
        }
    }
}

fun Route.gameEndpoints(gameService: CashGameService, authRepository: AuthRepository) {
    route("/game") {
        post("/join") {
            val token = call.request.queryParameters["token"] ?: throw IllegalStateException()
            val player = authRepository.getPlayer(token) ?: throw IllegalStateException()
            val tableId = gameService.createOrJoin(player.playerId, player.playerName)
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
            val token = call.parameters["token"] ?: throw IllegalStateException()
            val messageResponseFlow = MutableSharedFlow<HandEvent>()
            val sharedFlow = messageResponseFlow.asSharedFlow()

            val player = authRepository.getPlayer(token) ?: return@webSocket
            websockets[sessionId] = messageResponseFlow
            // TODO: [low] memory leak (this is never cleaned up)
            tableService.addWebSocketConnection(player.playerId, tableId, sessionId)

            val job = launch {
                sharedFlow.collect { message ->
                    sendSerialized(message)
                }
            }

            runCatching {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        val playerAction = json.decodeFromString<PlayerAction>(receivedText)
                        tableService.receivePlayerActions(
                            tableId,
                            player.playerId,
                            listOf(playerAction.toDomain(player.playerId))
                        )
                    }
                }
            }.onFailure { exception ->
                println("WebSocket exception: ${exception.localizedMessage}")
            }.also {
                job.cancel()
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun WebSocketServerSession.process(): List<PlayerAction> = buildList {
    while (true) {
        val result = incoming.tryReceive()
        val frame = result.getOrNull() ?: break
        frame as? Frame.Text ?: continue
        val receivedText = frame.readText()
        val playerAction = json.decodeFromString<PlayerAction>(receivedText)
        add(playerAction)
    }
}