package domain.table

import domain.model.Table
import domain.model.Table.Round.Action
import domain.model.Tournament
import domain.tournament.CashGameRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import server.PlayerActionRequest

class PokerScenarioBuilder(
    val players: Int,
    val smallBlind: Double,
    val bigBlind: Double,
    val seed: Long,
    val stacks: List<Double> = (0..8).map { 1000.0 },
    val cards: List<Table.Card>,
) {
    private var table: Table = createInitialTable()
    val now: Instant = wellKnownTimestamp
    private val seedGenerator = { seed }
    private val allActions = mutableListOf<Action>()

    fun createInitialTable(): Table {
        val playerList = (1..players).map {
            Tournament.Details.Player(it, "Player $it", stacks[it - 1])
        }
        val cashPlayers = playerList.map { CashGameRepository.Player(it.id, it.name, it.startingStack) }
        return createTable(
            players = cashPlayers,
            seed = seed,
            smallBlindAmount = smallBlind,
            bigBlindAmount = bigBlind,
            defaultCards = cards,
            minPlayers = 2,
            maxPlayers = 9,
        )
    }

    fun processTable(now: Instant = this.now) {
        table = table.processTable(now, seedGenerator)
    }

    fun p1() = PlayerActor(1, this)
    fun p2() = PlayerActor(2, this)
    fun p3() = PlayerActor(3, this)
    fun p4() = PlayerActor(4, this)
    fun p5() = PlayerActor(5, this)
    fun p6() = PlayerActor(6, this)
    fun p7() = PlayerActor(7, this)
    fun p8() = PlayerActor(8, this)
    fun p9() = PlayerActor(9, this)

    fun dealer(seat: Int) {
        table = table.copy(dealerSeat = seat)
    }

    fun stacks(amount: Double) {
        val playerList = (1..players).map {
            Tournament.Details.Player(it, "Player $it", amount)
        }
        val cashPlayers = playerList.map { CashGameRepository.Player(it.id, it.name, it.startingStack) }
        table = createTable(
            players = cashPlayers,
            seed = seed,
            smallBlindAmount = smallBlind,
            bigBlindAmount = bigBlind,
        ).copy(dealerSeat = table.dealerSeat)
    }

    fun manually(block: (Table) -> Unit) {
        block(table)
    }

    fun getPlayerStack(playerId: Int): Double = table.players.first { it.playerId == playerId }.stack

    fun advanceStreet(): PokerScenarioBuilder = apply {
        try {
            table = table.processPostAction(now)
        } catch (e: Exception) {
            // Table may not be ready for processPostAction yet
        }
    }

    fun nextStreet(): PokerScenarioBuilder = apply { processTable() }

    fun run(): PokerScenarioResult {
        allActions.clear()
        allActions += table.rounds.flatMap { it.actions }
        return PokerScenarioResult(table, allActions.toList())
    }

    internal fun doAction(playerId: Int, request: PlayerActionRequest) {
        table = table.processPlayerAction(playerId, request, now)
        table = table.processTable(now, seedGenerator)
        allActions.clear()
        allActions += table.rounds.flatMap { it.actions }
    }

    internal fun sitDown(playerId: Int, stack: Double) {
        table = table.addPlayer(CashGameRepository.Player(playerId, "Player $playerId", stack))
    }
}

class PlayerActor(val playerId: Int, val builder: PokerScenarioBuilder) {
    fun fold() = builder.doAction(playerId, PlayerActionRequest.Fold(playerId))
    fun check() = builder.doAction(playerId, PlayerActionRequest.Check(playerId))
    fun call(amount: Double) = builder.doAction(playerId, PlayerActionRequest.Call(playerId, amount))
    fun bet(amount: Double) = builder.doAction(playerId, PlayerActionRequest.Bet(playerId, amount))
    fun raise(amount: Double) = builder.doAction(playerId, PlayerActionRequest.Raise(playerId, amount))
    fun standUp() = builder.doAction(playerId, PlayerActionRequest.StandUp(playerId))
    fun sitDown(stack: Double) = builder.sitDown(playerId, stack)
}

fun pokerScenario(
    players: Int,
    blinds: Pair<Double, Double>,
    seed: Long = 1,
    stacks: List<Double> = (0..8).map { 1000.0 },
    cards: String? = null,
    scenario: PokerScenarioBuilder.() -> Unit,
): PokerScenarioResult {
    val builder =
        PokerScenarioBuilder(
            players,
            blinds.first,
            blinds.second,
            seed,
            stacks,
            cards?.split(" ")?.map { it.toCard() } ?: emptyList())
    builder.apply(scenario)
    return builder.run()
}

fun PokerScenarioBuilder.postBlinds() = apply { processTable() }
fun PokerScenarioBuilder.dealHoleCards() = apply { processTable() }
fun PokerScenarioBuilder.dealFlop() = apply { processTable() }
fun PokerScenarioBuilder.dealTurn() = apply { processTable() }
fun PokerScenarioBuilder.dealRiver() = apply { processTable() }
fun PokerScenarioBuilder.dealShowdown() = apply { processTable() }
fun PokerScenarioBuilder.nextStreet() = apply { processTable() }
fun PokerScenarioBuilder.startNextHand() = apply { processTable(now = now.plusSeconds(10)) }

data class PokerScenarioResult(
    val table: Table,
    val allActions: List<Action>,
) {
    fun assertFinished() = assertTrue(table.isFinished)
    fun assertNotFinished() = assertTrue(!table.isFinished)
    fun assertPot(amount: Double) {
        assertEquals(1, table.pots.size)
        assertEquals(amount, table.pots[0].amount)
    }

    fun assertPotCount(count: Int) = assertEquals(count, table.pots.size)
    fun assertPlayerWins(playerId: Int, amount: Double) {
        val pot = table.pots.firstOrNull()
        assertTrue(pot != null, "Expected a pot")
        val win = pot!!.playerWins.firstOrNull { it.playerId == playerId }
        assertTrue(win != null, "Expected player $playerId to win")
        assertEquals(amount, win!!.winAmount)
    }

    fun assertFinishedPots(vararg pots: Pair<Int, Double>) {
        val ended = allActions.filterIsInstance<Action.HandEnded>().single()
        val expectedStacks = pots.map { Action.PlayerStack(it.first, it.second) }
        assertEquals(expectedStacks.sortedBy { it.playerId }, ended.playerStacks.toSet().sortedBy { it.playerId })
        assertEquals(expectedStacks.sortedBy { it.playerId }, table.players.map { Action.PlayerStack(it.playerId, it.stack) }.toSet().sortedBy { it.playerId })
    }

    fun assertCommunityCards(vararg cards: String) {
        val communityCards = table.rounds
            .flatMap { it.actions }
            .filterIsInstance<Action.DealCommunityCards>()
            .flatMap { it.cards }
        assertEquals(cards.size, communityCards.size)
        cards.forEachIndexed { index, cardStr ->
            assertEquals(cardStr.toCard(), communityCards[index])
        }
    }

    fun assertPlayerCards(playerId: Int, vararg cards: String) {
        val player = table.players.firstOrNull { it.playerId == playerId }
        assertTrue(player != null, "Expected player $playerId")
        assertEquals(cards.size, player!!.cards.size)
        cards.forEachIndexed { index, cardStr ->
            assertEquals(cardStr.toCard(), player.cards[index])
        }
    }

    fun assertStack(playerId: Int, amount: Double) {
        val player = table.players.firstOrNull { it.playerId == playerId }
        assertTrue(player != null, "Expected player $playerId")
        assertEquals(amount, player!!.stack)
    }

    fun assertStreet(street: Table.Round.Street) {
        assertEquals(street, table.currentRound?.street)
    }

    fun assertLastPlayerToAct(playerId: Int) {
        val lastAction = table.playerRoundActions.lastOrNull()
        assertTrue(lastAction != null, "Expected a player action")
        assertEquals(playerId, lastAction!!.playerId)
    }
}

fun String.toCard(): Table.Card {
    val suitChar = this.last()
    val suit = when (suitChar) {
        'h' -> Table.Card.Suit.Hearts
        'd' -> Table.Card.Suit.Diamonds
        's' -> Table.Card.Suit.Spades
        'c' -> Table.Card.Suit.Clubs
        else -> throw IllegalStateException("Invalid suit: $suitChar")
    }
    val rank = this.dropLast(1).toInt()
    return Table.Card(suit, rank)
}