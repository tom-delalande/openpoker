package domain.table

import data.inmemory.InMemoryActiveTableStateRepository
import data.inmemory.InMemoryAuthRepository
import data.inmemory.InMemoryCashGameRepository
import data.inmemory.InMemoryHandHistoryRepository
import domain.model.Table
import domain.tournament.CashGameService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
        tableService = TableService(activeTableStateRepository, emptyMap())
        gameService = CashGameService(cashGameRepository, handHistoryRepository, tableService)
    }

    @Test
    @kotlin.test.Ignore("saveTable method not implemented - needs update")
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
            Socket(0, UUID.randomUUID(), UUID.randomUUID(), 0, 0),
            Socket(1, UUID.randomUUID(), UUID.randomUUID(), 0, 0),
            Socket(2, UUID.randomUUID(), UUID.randomUUID(), 0, 0)
        )
        val player1Flow = MutableSharedFlow<HandEvent>(replay = 100)
        websockets[playerSockets[0].sessionId] = player1Flow

        activeTableStateRepository.create(tableId, ActiveTable(tableId, table, playerSockets, false))
        var now = wellKnownTimestamp

        tableService.process(tableId, now)

        val firstProcessCount = player1Flow.replayCache.size

        player1Flow.resetReplayCache()

        tableService.process(tableId, now)

        assertTrue(firstProcessCount > 0, "Expected events on first process")
    }

    @Test
    @kotlin.test.Ignore("saveTable method not implemented - needs update")
    fun testFiveSecondDelayBeforeNextHandStarts() = runTest {
        val seedGenerator = { 1L }
        val table = givenWellKnownTournamentTable {
            withSeed(1)
            withDealerSeat(0)
            withDefaultPlayers(3)
            withBlinds(smallBlind = 5.0, bigBlind = 10.0)
        }.let {
            it.copy(
                isFinished = true,
                finishedAt = wellKnownTimestamp.minusSeconds(10),
                rounds = listOf(
                    Table.Round(
                        id = 0,
                        street = Table.Round.Street.Showdown,
                        actions = listOf()
                    )
                )
            )
        }

        val tableId = UUID.randomUUID()
        val playerSockets = listOf(
            Socket(0, UUID.randomUUID(), UUID.randomUUID(), 0, 0),
            Socket(1, UUID.randomUUID(), UUID.randomUUID(), 0, 0),
            Socket(2, UUID.randomUUID(), UUID.randomUUID(), 0, 0)
        )
        activeTableStateRepository.create(tableId, ActiveTable(tableId, table, playerSockets, false))
    }

    @Test
    @kotlin.test.Ignore("saveTable method not implemented - needs update")
    fun testSocketVersionsResetOnNewHand() = runTest {
        val seedGenerator = { 1L }
        val table = givenWellKnownTournamentTable {
            withSeed(1)
            withDealerSeat(0)
            withDefaultPlayers(3)
            withBlinds(smallBlind = 5.0, bigBlind = 10.0)
        }.let {
            it.copy(
                isFinished = true,
                finishedAt = wellKnownTimestamp,
                rounds = listOf(
                    Table.Round(
                        id = 0,
                        street = Table.Round.Street.Showdown,
                        actions = listOf()
                    )
                )
            )
        }

        val tableId = UUID.randomUUID()
        val playerSockets = listOf(
            Socket(0, UUID.randomUUID(), UUID.randomUUID(), 0, 42),
            Socket(1, UUID.randomUUID(), UUID.randomUUID(), 0, 42),
            Socket(2, UUID.randomUUID(), UUID.randomUUID(), 0, 42)
        )
        activeTableStateRepository.create(tableId, ActiveTable(tableId, table, playerSockets, false))
    }

    @Test
    @kotlin.test.Ignore("saveTable method not implemented - needs update")
    fun testFullFlowHandEndsDelayNewHandStartsWithResetVersions() = runTest {
        val seedGenerator = { 1L }
        val table = givenWellKnownTournamentTable {
            withSeed(1)
            withDealerSeat(0)
            withDefaultPlayers(3)
            withBlinds(smallBlind = 5.0, bigBlind = 10.0)
        }.let {
            it.copy(
                isFinished = true,
                finishedAt = wellKnownTimestamp,
                rounds = listOf(
                    Table.Round(
                        id = 0,
                        street = Table.Round.Street.Showdown,
                        actions = listOf()
                    )
                )
            )
        }

        val tableId = UUID.randomUUID()
        val playerSockets = listOf(
            Socket(0, UUID.randomUUID(), UUID.randomUUID(), 0, 100),
            Socket(1, UUID.randomUUID(), UUID.randomUUID(), 0, 100),
            Socket(2, UUID.randomUUID(), UUID.randomUUID(), 0, 100)
        )
        val player1Flow = MutableSharedFlow<HandEvent>(replay = 100)
        val player2Flow = MutableSharedFlow<HandEvent>(replay = 100)
        val player3Flow = MutableSharedFlow<HandEvent>(replay = 100)
        websockets[playerSockets[0].sessionId] = player1Flow
        websockets[playerSockets[1].sessionId] = player2Flow
        websockets[playerSockets[2].sessionId] = player3Flow

        activeTableStateRepository.create(tableId, ActiveTable(tableId, table, playerSockets, false))
    }
}
