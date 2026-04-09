package server

import app.WebSocketId
import domain.model.Table
import domain.table.TableService
import domain.tournament.CashGameService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import server.models.PlayerAction

private val json = Json

fun Route.authEndpoints(authRepository: AuthRepository) {
    route("/auth") {
        post("/login/{name}") {
            val name = call.parameters["name"] ?: throw IllegalStateException()
            val playerId = Random.nextInt()
            val token = UUID.randomUUID().toString()
            authRepository.saveToken(token, playerId, name)
            call.respond(token)
        }
    }
}

fun Route.gameEndpoints(gameService: CashGameService) {
    route("/game") {

    }
}

fun Route.tableEndpoints(
    websockets: ConcurrentHashMap<WebSocketId, DefaultWebSocketServerSession>,
    authRepository: AuthRepository,
    tableService: TableService,
) {


    route("/table") {
        webSocket("/game/{gameId}/token/{token}") {
            val gameId = call.parameters["gameId"]?.let { UUID.fromString(it) } ?: throw IllegalStateException()
            val token = call.parameters["token"] ?: throw IllegalStateException()

            val player = authRepository.getPlayer(token) ?: throw IllegalStateException()
            websockets[WebSocketId(player.playerId, gameId)] = this@webSocket
            while (true) {

            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun WebSocketServerSession.process(): List<PlayerAction> = buildList {
    for (frame in incoming) {
        frame as? Frame.Text ?: continue
        val receivedText = frame.readText()
        val playerAction = json.decodeFromString<PlayerAction>(receivedText)
        add(playerAction)
    }
}