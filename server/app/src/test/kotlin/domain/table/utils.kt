package domain.table

import domain.model.Round
import domain.model.Table
import domain.model.Tournament
import domain.tournament.CashGameRepository
import java.time.Instant
import kotlin.collections.map

internal val wellKnownTournamentDetails = Tournament.Details(
    name = "Tournament 1",
    startTime = null,
    type = Tournament.Details.TournamentType.SingleTableTournament,
    speed = Tournament.Details.Speed(
        type = Tournament.Details.Speed.SpeedType.Normal,
        roundTime = 60,
    ),
    startingStack = 1000.0,
    players = listOf(),
    status = Tournament.Details.Status.Started,
)

internal val wellKnownTimestamp = Instant.parse("2026-04-06T11:53:19.312063Z")

fun givenWellKnownTournamentTable(work: TableConfig.() -> Unit): Table {
    val config = TableConfig()
    work(config)
    val players = config.players.map { domain.tournament.CashGameRepository.Player(it.id, it.name, it.startingStack) }
    return createTable(players = players, seed = config.seed)
        .copy(
            dealerSeat = config.dealerSeat,
            rounds = config.rounds,
        )
}

data class TableConfig(
    var players: List<Tournament.Details.Player> = emptyList(),
    var dealerSeat: Int = 0,
    var seed: Long = 1,
    var rounds: MutableList<Table.Round> = mutableListOf(),
)

fun TableConfig.withSeed(seed: Long) {
    this.seed = seed
}

fun TableConfig.withDealerSeat(dealerSeat: Int) {
    this.dealerSeat = dealerSeat
}

fun TableConfig.withPlayers(vararg players: Tournament.Details.Player) {
    this.players = players.toList()
}

fun TableConfig.withDefaultPlayers(number: Int, startingStack: Double = 1000.0) {
    this.players = (1..number).map {
        Tournament.Details.Player(
            id = it,
            name = "Player $it",
            startingStack = startingStack,
        )
    }
}

fun TableConfig.withRound(street: Table.Round.Street, work: RoundConfig.() -> Unit) {
    val config = RoundConfig()
    work(config)
    rounds.add(
        Table.Round(
            id = rounds.size,
            street = street,
            actions = config.actions,
        )
    )
}

fun TableConfig.withDealtCards() {
    val firstSeat = (dealerSeat + 1) % players.size
    val orderedPLayers = (firstSeat..<firstSeat + players.size).map { it % players.size }
    withRound(Table.Round.Street.PreFlop) {
        orderedPLayers.forEach { index ->
            val player = players[index]
            val cards = getCards(seed, index * 2, 2)
            withAction(
                Table.Round.Action.PlayerAction.DealCards(
                    playerId = player.id,
                    cards = cards,
                )
            )
        }
    }
}

fun TableConfig.withAction(action: Table.Round.Action) {
    val round = rounds.removeLast()
    val updated = round.copy(
        actions = round.actions + action
    )
    rounds.add(updated)
}

fun TableConfig.withPreFlopRound() {
    withRound(Table.Round.Street.PreFlop) {
        players.forEachIndexed { index, player ->
            val cards = getCards(seed, index * 2, 2)
            withAction(
                Table.Round.Action.PlayerAction.DealCards(
                    playerId = player.id,
                    cards = cards,
                )
            )
        }
    }
}

data class RoundConfig(
    var actions: MutableList<Table.Round.Action> = mutableListOf(),
    var cards: MutableList<Table.Card> = mutableListOf(),
)

fun RoundConfig.withAction(action: Table.Round.Action) {
    actions.add(action)
}


