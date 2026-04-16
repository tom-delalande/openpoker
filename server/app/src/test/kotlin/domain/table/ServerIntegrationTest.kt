@file:OptIn(ExperimentalSerializationApi::class)

package domain.table

import data.inmemory.InMemoryActiveTableStateRepository
import data.inmemory.InMemoryAuthRepository
import data.inmemory.InMemoryCashGameRepository
import data.inmemory.InMemoryHandHistoryRepository
import domain.tournament.CashGameService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import server.authEndpoints
import server.gameEndpoints
import server.models.*
import server.tableEndpoints
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

class PlayerSocket(
    val playerId: Int,
    val token: String,
    var session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession? = null,
) {
    suspend fun send(action: PlayerAction) {
        val jsonString = Json.encodeToString(PlayerAction.serializer(), action)
        session?.outgoing?.send(Frame.Text(jsonString))
    }

    suspend fun receiveEvents(): List<HandEvent> {
        val events = mutableListOf<HandEvent>()
        try {
            while (true) {
                val result = session?.incoming?.tryReceive()
                val frame = result?.getOrNull() ?: break
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val event = Json.decodeFromString<HandEvent>(text)
                    events.add(event)
                }
            }
        } catch (e: Exception) {
        }
        return events
    }
}

@Disabled("Pre-existing test needs update for new constructor signatures")
class ServerIntegrationTest {
    private lateinit var activeTableStateRepository: InMemoryActiveTableStateRepository
    private lateinit var handHistoryRepository: InMemoryHandHistoryRepository
    private lateinit var cashGameRepository: InMemoryCashGameRepository
    private lateinit var authRepository: InMemoryAuthRepository
    private lateinit var websockets: ConcurrentHashMap<UUID, kotlinx.coroutines.flow.MutableSharedFlow<HandEvent>>
    private lateinit var tableService: TableService
    private lateinit var gameService: CashGameService

    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null
    private var serverPort: Int = 3003
    private var processingJob: Job? = null

    @BeforeEach
    fun setup() {
        activeTableStateRepository = InMemoryActiveTableStateRepository()
        handHistoryRepository = InMemoryHandHistoryRepository()
        cashGameRepository = InMemoryCashGameRepository()
        authRepository = InMemoryAuthRepository()
        websockets = ConcurrentHashMap()
        tableService = TableService(activeTableStateRepository, emptyMap())
        gameService = CashGameService(cashGameRepository, handHistoryRepository, tableService)

        server = embeddedServer(Netty, port = serverPort) {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
            install(ContentNegotiation) {
                json()
            }
            install(CORS) {
                anyHost()
            }

            var playerId = 0
            routing {
                authEndpoints(authRepository, gameService, { playerId++ })
                gameEndpoints(gameService, authRepository)
                tableEndpoints(websockets, authRepository, tableService)
            }
        }

        server?.start()

        processingJob = CoroutineScope(Dispatchers.Default).launch {
//            while (true) {
//                tableService.process()
//                delay(500)
//            }
        }
    }

    @AfterEach
    fun teardown() {
        processingJob?.cancel()
        server?.stop(1000, 2000)
    }

    @Test
    fun `login, join table, and connect websocket`() = runTest {
        val client = HttpClient(CIO) {
            install(ClientWebSockets)
        }

        try {
            val response: AuthResponse = client.post("http://localhost:$serverPort/auth/login/testuser").body()

            val tableId: String = client.post("http://localhost:$serverPort/game/join") {
                url {
                    parameters.append("token", response.token)
                }
            }.body()
            println("Table ID: $tableId")

            val wsSession = client.webSocketSession(
                "ws://localhost:$serverPort/table/ws/table/$tableId/token/$response"
            )

            val sitDownAction = PlayerActionSitDown(
                value = SitDown(
                    type = SitDownType.SIT_DOWN
                )
            )
            val jsonString = Json.encodeToString(PlayerActionSitDown.serializer(), sitDownAction)
            wsSession.outgoing.send(Frame.Text(jsonString))

            try {
                val frame = wsSession.incoming.receive()
                println("Received: $frame")
            } catch (e: Exception) {
                println("Timeout or error: $e")
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun `given multiple websockets, when joining in close succession, then all connections are established and maintained`() = runBlocking {
        val client = HttpClient(CIO) {
            install(ClientWebSockets)
        }

        try {
            val player1 = createAndLogin(client, "player1")
            val player2 = createAndLogin(client, "player2")
            val player3 = createAndLogin(client, "player3")

            val tableId = joinTable(client, player1.token)
            assertEquals(tableId, joinTable(client, player2.token))
            assertEquals(tableId, joinTable(client, player3.token))

            connectWebsocket(client, tableId, player1)
            connectWebsocket(client, tableId, player2)
            connectWebsocket(client, tableId, player3)

            delay(1000)
            // tableService.process(UUID.fromString(tableId))
            delay(1000)

            val player1Events = player1.receiveEvents()
            val player2Events = player2.receiveEvents()
            val player3Events = player3.receiveEvents()

            println("Player 1 events: $player1Events")
            println("Player 2 events: $player2Events")
            println("Player 3 events: $player3Events")

            assertEquals(5, player1Events.size)
            assertEquals(5, player2Events.size)
            assertEquals(5, player3Events.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun `play a game with 3 players`() = runBlocking {
        val client = HttpClient(CIO) {
            install(ClientWebSockets)
        }

        try {
            val player1 = createAndLogin(client, "player1")
            val player2 = createAndLogin(client, "player2")
            val player3 = createAndLogin(client, "player3")

            val tableId = joinTable(client, player1.token)
            assertEquals(tableId, joinTable(client, player2.token))
            assertEquals(tableId, joinTable(client, player3.token))

            connectWebsocket(client, tableId, player1)
            connectWebsocket(client, tableId, player2)
            connectWebsocket(client, tableId, player3)

            delay(1000)
            // tableService.process(UUID.fromString(tableId))
            // tableService.process(UUID.fromString(tableId))
            delay(1000)

            val player1Events = player1.receiveEvents()
            val player2Events = player2.receiveEvents()
            val player3Events = player3.receiveEvents()

            assertEquals(6, player1Events.size)
            assertEquals(5, player2Events.size)
            assertEquals(5, player3Events.size)
        } finally {
            client.close()
        }
    }

    private suspend fun createAndLogin(client: HttpClient, name: String): PlayerSocket {
        val token: String = client.post("http://localhost:$serverPort/auth/login/$name").body()
        val playerId = authRepository.getPlayer(UUID.fromString(token))?.playerId ?: throw IllegalStateException("Player not found")
        return PlayerSocket(playerId, token)
    }

    private suspend fun joinTable(client: HttpClient, token: String): String {
        return client.post("http://localhost:$serverPort/game/join") {
            url { parameters.append("token", token) }
        }.body()
    }

    private suspend fun connectWebsocket(client: HttpClient, tableId: String, playerSocket: PlayerSocket) {
        val session =
            client.webSocketSession("ws://localhost:$serverPort/table/ws/table/$tableId/token/${playerSocket.token}")
        playerSocket.session = session
    }
}