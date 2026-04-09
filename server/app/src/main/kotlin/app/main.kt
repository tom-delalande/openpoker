package app

import data.inmemory.InMemoryActiveTableStateRepository
import data.inmemory.InMemoryAuthRepository
import data.inmemory.InMemoryCashGameRepository
import data.inmemory.InMemoryHandHistoryRepository
import domain.table.TableService
import domain.tournament.CashGameService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import server.authEndpoints
import server.gameEndpoints
import server.process
import server.tableEndpoints
import server.toDomain

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val activeTableStateRepository = InMemoryActiveTableStateRepository()
    val handHistoryRepository = InMemoryHandHistoryRepository()
    val cashGameRepository = InMemoryCashGameRepository()
    val authRepository = InMemoryAuthRepository()

    val websockets = ConcurrentHashMap<WebSocketId, DefaultWebSocketServerSession>()
    val tableService = TableService(activeTableStateRepository, handHistoryRepository, websockets)
    val gameService = CashGameService(cashGameRepository, tableService)

    CoroutineScope(Dispatchers.Default).launch {
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

    embeddedServer(Netty, port = 3001) {
        install(WebSockets)
        install(ContentNegotiation) {
            json()
        }

        routing {
            authEndpoints(authRepository)
            gameEndpoints(gameService, authRepository)
            tableEndpoints(websockets, authRepository, tableService)
        }
    }.start(wait = true)
}

data class WebSocketId(
    val playerId: Int,
    val tableId: UUID,
)