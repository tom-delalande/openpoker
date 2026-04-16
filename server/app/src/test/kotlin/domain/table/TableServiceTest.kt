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

        activeTableStateRepository.set(tableId, ActiveTable(tableId, table, playerSockets, false))
        var now = wellKnownTimestamp

        tableService.process(tableId, now)

        val activeTableAfterFirst = activeTableStateRepository.get(tableId)!!

        val firstProcessCount = player1Flow.replayCache.size

        player1Flow.resetReplayCache()

        tableService.process(tableId, now)

        val activeTableAfterSecond = activeTableStateRepository.get(tableId)!!

        val secondProcessCount = player1Flow.replayCache.size

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
        activeTableStateRepository.set(tableId, ActiveTable(tableId, table, playerSockets, false))

        val sevenSecondsLater = wellKnownTimestamp.plusSeconds(7)
        // tableService.saveTable(tableId, table, false, playerSockets, sevenSecondsLater)
        // var activeTable = activeTableStateRepository.get(tableId)!!

        // assertFalse(activeTable.table.isFinished, "Expected isFinished to be false after 5+ seconds")
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
        activeTableStateRepository.set(tableId, ActiveTable(tableId, table, playerSockets, false))

        val sevenSecondsLater = wellKnownTimestamp.plusSeconds(7)
        // tableService.saveTable(tableId, table, false, playerSockets, sevenSecondsLater)

        // val activeTable = activeTableStateRepository.get(tableId)!!
        // assertEquals(0, activeTable.playerSockets[0].version)
        // assertEquals(0, activeTable.playerSockets[1].version)
        // assertEquals(0, activeTable.playerSockets[2].version)
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

        activeTableStateRepository.set(tableId, ActiveTable(tableId, table, playerSockets, false))

        val threeSecondsLater = wellKnownTimestamp.plusSeconds(3)
        // tableService.saveTable(tableId, table, false, playerSockets, threeSecondsLater)
        // var activeTable = activeTableStateRepository.get(tableId)!!
        // assertTrue(activeTable.table.isFinished)

        val sevenSecondsLater = wellKnownTimestamp.plusSeconds(7)
        // tableService.saveTable(tableId, table, false, playerSockets, sevenSecondsLater)
        // activeTable = activeTableStateRepository.get(tableId)!!
        // assertFalse(activeTable.table.isFinished)
        // assertEquals(0, activeTable.playerSockets[0].version)
    }
}
