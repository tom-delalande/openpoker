package app

import data.inmemory.InMemoryActiveTableStateRepository
import data.inmemory.InMemoryAuthRepository
import data.inmemory.InMemoryCashGameRepository
import data.inmemory.InMemoryHandHistoryRepository
import domain.table.TableService
import domain.tournament.CashGameService
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import server.authEndpoints
import server.gameEndpoints
import server.models.HandEvent
import server.tableEndpoints

val logger = LoggerFactory.getLogger("Main")

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val activeTableStateRepository = InMemoryActiveTableStateRepository()
    val handHistoryRepository = InMemoryHandHistoryRepository()
    val cashGameRepository = InMemoryCashGameRepository()
    val authRepository = InMemoryAuthRepository()

    val websockets = ConcurrentHashMap<WebSocketId, MutableSharedFlow<HandEvent>>()
    val tableService = TableService(activeTableStateRepository, handHistoryRepository, websockets)
    val gameService = CashGameService(cashGameRepository, tableService)

    CoroutineScope(Dispatchers.Default).launch {
        val logger = LoggerFactory.getLogger("ProcessingThread")
        while (true) {
            try {
                tableService.process()
                delay(500.milliseconds)
            } catch (exception: Exception) {
                logger.error("error when processing", exception)
            }
        }
    }

    embeddedServer(Netty, port = 3001) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            anyHost()
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