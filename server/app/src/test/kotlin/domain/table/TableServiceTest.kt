package domain.table

import data.inmemory.InMemoryActiveTableStateRepository
import data.inmemory.InMemoryAuthRepository
import data.inmemory.InMemoryCashGameRepository
import data.inmemory.InMemoryHandHistoryRepository
import domain.tournament.CashGameService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import server.PlayerActionRequest
import server.models.HandEvent

@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
class TableServiceTest {
    private lateinit var activeTableStateRepository: InMemoryActiveTableStateRepository
    private lateinit var handHistoryRepository: InMemoryHandHistoryRepository
    private lateinit var cashGameRepository: InMemoryCashGameRepository
    private lateinit var authRepository: InMemoryAuthRepository
    private lateinit var websockets: ConcurrentHashMap<UUID, MutableSharedFlow<HandEvent>>
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
        tableService = TableService(activeTableStateRepository, handHistoryRepository, websockets)
        gameService = CashGameService(cashGameRepository, tableService)
    }

    @Test
    fun testPlayerEvents() = runTest {
        val seedGenerator = { 1L }
        var table = givenWellKnownTournamentTable {
            withSeed(1)
            withDealerSeat(0)
            withDefaultPlayers(3)
            withBlinds(
                smallBlind = 5.0,
                bigBlind = 10.0,
            )
        }

        val tableId = UUID.randomUUID()
        val playerSockets = listOf(
            Socket(
                0,
                UUID.randomUUID(),
                0,
            ),
            Socket(
                1,
                UUID.randomUUID(),
                0,
            ),
            Socket(
                2,
                UUID.randomUUID(),
                0,
            )
        )
        val player1Flow = MutableSharedFlow<HandEvent>(replay = 100)
        val player2Flow = MutableSharedFlow<HandEvent>(replay = 100)
        val player3Flow = MutableSharedFlow<HandEvent>(replay = 100)
        websockets[playerSockets[0].sessionId] = player1Flow
        websockets[playerSockets[1].sessionId] = player2Flow
        websockets[playerSockets[2].sessionId] = player3Flow

        activeTableStateRepository.set(tableId, table, playerSockets)
        var now = wellKnownTimestamp

        tableService.process(now, seedGenerator)
        tableService.process(now, seedGenerator)

        assertEquals(listOf(), player1Flow.replayCache)
        assertEquals(listOf(), player2Flow.replayCache)
        assertEquals(listOf(), player2Flow.replayCache)
        player1Flow.resetReplayCache()
        player2Flow.resetReplayCache()
        player2Flow.resetReplayCache()

        tableService.receivePlayerActions(
            tableId,
            2,
            listOf(PlayerActionRequest.PostSmallBlind(2, 5.0)),
            now,
        )

        assertEquals(listOf(), player1Flow.replayCache)
        assertEquals(listOf(), player2Flow.replayCache)
        assertEquals(listOf(), player2Flow.replayCache)
        player1Flow.resetReplayCache()
        player2Flow.resetReplayCache()
        player2Flow.resetReplayCache()
    }
}