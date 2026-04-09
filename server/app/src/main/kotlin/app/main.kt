package app

import data.inmemory.InMemoryActiveTableStateRepository
import data.inmemory.InMemoryAuthRepository
import data.inmemory.InMemoryCashGameRepository
import data.inmemory.InMemoryHandHistoryRepository
import domain.table.TableService
import domain.tournament.CashGameService
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import server.authEndpoints
import server.gameEndpoints
import server.process
import server.tableEndpoints
import server.toDomain

@OptIn(ExperimentalSerializationApi::class)
suspend fun main() {
    val activeTableStateRepository = InMemoryActiveTableStateRepository()
    val handHistoryRepository = InMemoryHandHistoryRepository()
    val cashGameRepository = InMemoryCashGameRepository()
    val authRepository = InMemoryAuthRepository()

    val tableService = TableService(activeTableStateRepository, handHistoryRepository)
    val gameService = CashGameService(cashGameRepository, tableService)

    val websockets = ConcurrentHashMap<WebSocketId, DefaultWebSocketServerSession>()

    coroutineScope {
        launch {
            while (true) {
                for (websocket in websockets) {
                    val (playerId, tableId) = websocket.key
                    val session = websocket.value

                    val actions = session.process()
                    tableService.receivePlayerActions(tableId, playerId, actions.map { it.toDomain(playerId) })
                }
                tableService.process()
                delay(500.milliseconds)
            }
        }
    }

    embeddedServer(Netty, port = 9090) {
        install(WebSockets)
        routing {
            route("/api") {
                authEndpoints(authRepository)
                gameEndpoints(gameService)
                tableEndpoints(websockets, authRepository, tableService)
            }
        }
    }.start(wait = true)
}

data class WebSocketId(
    val playerId: Int,
    val tableId: UUID,
)