package domain.table

import domain.model.Table
import domain.model.Table.Round
import domain.model.Tournament
import java.time.Instant
import domain.model.Table.Round.Action.PlayerAction.RequestAction.ActionOption as ActionOption
import domain.model.Table.Round.Action.PlayerAction.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

const val DEFAULT_SMALL_BLIND_AMOUNT = 5.0
const val DEFAULT_BIG_BLIND_AMOUNT = 5.0
const val DEFAULT_TIMEOUT_IN_SECONDS = 10L

fun createTable(
    tournamentDetails: Tournament.Details,
    seed: Long = Random.nextLong(),
): Table {
    val table = Table(
        gameType = Table.GameType.HoldEm,
        betLimit = Table.BetLimit(
            betType = Table.BetLimit.BetType.NoLimit,
            betCap = 10.0,
        ),
        tableSize = tournamentDetails.players.size,
        dealerSeat = 0,
        smallBlindAmount = DEFAULT_SMALL_BLIND_AMOUNT,
        bigBlindAmount = DEFAULT_BIG_BLIND_AMOUNT,
        anteAmount = 6.0,
        players = tournamentDetails.players.mapIndexed { index, player ->
            Table.Player(
                id = player.id,
                name = player.name,
                seat = index,
                startingStack = tournamentDetails.startingStack,
                isSittingOut = false,
            )
        },
        rounds = listOf(),
        pots = listOf(),
        seed = seed,
    )

    return table
}

fun Table.dealInitialCards(): Table {
    return copy(
        rounds = listOf(
            Round(
                id = 0,
                street = Round.Street.PreFlop,
                cards = emptyList(),
                actions = emptyList(),
            )
        )
    ).dealCards()
}

private fun Table.dealCards(): Table {
    val cards = getCards(players.size * 2)
    val firstSeat = dealerSeat.nextSeat()
    val orderedPLayers = (firstSeat..<firstSeat + players.size).map { it % players.size }
    return orderedPLayers.fold(this) { table, seat ->
        val player = players[seat]
        table.appendAction(
            DealCards(
                playerId = player.id,
                cards = cards.subList(2 * seat, 2 * seat + 2)
            )
        )
    }
}

fun Table.processTable(now: Instant): Table {
    return when (val latestAction = currentRound.actions.last()) {
        is RequestAction -> {
            if (latestAction.expiry.isBefore(now)) {
                this
                    .timeoutCurrentActionRequest(latestAction)
                    .processPostAction(now)
            } else {
                this
            }
        }

        else -> {
            processPostAction(now)
        }
    }
}

fun Table.processPostAction(now: Instant): Table = attemptFinishRound().requestNextAction(now)

// TODO: Maybe this action type should be it's own interface
fun Table.processPlayerAction(playerId: Int, action: Round.Action.PlayerAction, now: Instant): Table {
    val lastAction = playerActions.last()
    if (lastAction !is RequestAction) {
        throw IllegalStateException("Unexpected action. playerId[$playerId] action[$action] lastEvent[$lastAction]")
    }
    if (lastAction.playerId != playerId) {
        throw IllegalStateException("Unexpected action for player. playerId[$playerId] expectedPlayerId[${lastAction.playerId}]")
    }

    // TODO: Check action is in action options

    return appendAction(action)
        .processPostAction(now)
}

private fun Table.getCards(numberOfCards: Int): List<Table.Card> {
    return getCards(seed, currentNumberOfCards, numberOfCards)
}

fun getCards(seed: Long, offset: Int, numberOfCards: Int): List<Table.Card> {
    return Table.Card.Suit.entries.toList().flatMap { suit ->
        (1..13).map { rank ->
            Table.Card(suit, rank)
        }
    }.shuffled(Random(seed)).subList(offset, offset + numberOfCards)
}

private fun Table.attemptFinishRound(): Table {
    if (currentRound.street == Round.Street.Showdown) {
        // Do Showdown stuff here
    }
    val lastAction = playerActions.filterNot { it is RequestAction }.last()
    val lastActionPlayer = players.find { it.id == lastAction.playerId }
    val lastRaiseAction = playerActions.findLast { it is Raise || it is PostSmallBlind || it is PostBigBlind }

    if (lastRaiseAction?.playerId == lastActionPlayer?.id) {
        if (currentRound.street == Round.Street.River) {
            // Finish Hand
            return this
        } else {
            val street = when (currentRound.street) {
                Round.Street.PreFlop -> Round.Street.Flop
                Round.Street.Flop -> Round.Street.Turn
                Round.Street.Turn -> Round.Street.River
                Round.Street.River,
                Round.Street.Showdown,
                    -> throw IllegalStateException("Cannot go to next round, you should have finished hand.")
            }
            return copy(
                rounds = rounds.plus(
                    Round(
                        id = currentRound.id + 1,
                        street = street,
                        cards = when (street) {
                            Round.Street.PreFlop -> emptyList()
                            Round.Street.Flop -> getCards(3)
                            Round.Street.Turn -> getCards(1)
                            Round.Street.River -> getCards(1)
                            Round.Street.Showdown -> emptyList()
                        },
                        actions = listOf(

                        )
                    )
                )
            )
        }
    }
    return this
}

private fun Table.requestNextAction(now: Instant): Table {
    val lastAction = currentRound.actions.filterIsInstance<Round.Action.PlayerAction>().lastOrNull()
    val lastActionPlayer = players.find { it.id == lastAction?.playerId } ?: players[dealerSeat]

    val nextPlayer = players[players.indexOf(lastActionPlayer).nextSeat()]

    val playerRaise = currentRaiseByPlayer[nextPlayer.id] ?: 0.0
    val playerStack = currentStackByPlayer[nextPlayer.id]!!

    val actionOptions = buildList {
        if (currentRound.street == Round.Street.PreFlop && nextPlayer.id == smallBlindPlayer.id && playerActions.filterIsInstance<PostSmallBlind>()
                .none()
        ) {
            add(ActionOption.Fold)
            add(ActionOption.PostSmallBlind(amount = min(smallBlindAmount, playerStack)))
            return@buildList
        }

        if (currentRound.street == Round.Street.PreFlop && nextPlayer.id == bigBlindPlayer.id && playerActions.filterIsInstance<PostBigBlind>()
                .none()
        ) {
            add(ActionOption.Fold)
            add(ActionOption.PostBigBlind(amount = min(bigBlindAmount, playerStack)))
            return@buildList
        }

        if (playerRaise <= currentRaise) {
            add(ActionOption.Fold)
        }

        if (playerRaise >= currentRaise) {
            add(ActionOption.Check)
        }

        if (currentRaise == 0.0 && playerStack > 0.0) {
            add(ActionOption.Bet(minAmount = bigBlindAmount, maxAmount = playerStack))
        }

        if (currentRaise > 0.0 && playerStack > 0.0) {
            add(
                ActionOption.Raise(
                    minAmount = currentRaise + bigBlindAmount,
                    maxAmount = max(playerRaise - currentRaise, playerStack)
                )
            )
        }

        if (currentRaise > 0.0 && playerRaise < currentRaise && playerStack > 0.0) {
            add(ActionOption.Call(amount = min(playerStack, max(playerStack, playerRaise - currentRaise))))
        }
    }


    val actionRequest = RequestAction(
        playerId = nextPlayer.id,
        actionOptions = actionOptions,
        expiry = now.plusSeconds(10),
    )
    return appendAction(actionRequest)
}

private fun Table.timeoutCurrentActionRequest(latestAction: RequestAction): Table {
    val defaultAction = latestAction.actionOptions.first()
    val playerId = latestAction.playerId

    val newAction = when (defaultAction) {
        ActionOption.Check -> Check(
            playerId = playerId,
        )

        ActionOption.Fold -> Fold(
            playerId = playerId,
        )

        is ActionOption.Call,
        is ActionOption.Bet,
        is ActionOption.PostAnte,
        is ActionOption.PostBigBlind,
        is ActionOption.PostDeadBlind,
        is ActionOption.PostExtraBlind,
        is ActionOption.PostSmallBlind,
        is ActionOption.PostStraddle,
        is ActionOption.Raise,
        ActionOption.MuckCards,
        ActionOption.ShowCards,
            -> throw IllegalStateException("Unexpected default action: $defaultAction")

    }
    return appendAction(newAction)
}

private fun Table.appendAction(action: Round.Action): Table {
    val currentRoundId = rounds.last().id
    return copy(
        rounds = rounds.map
        {
            if (it.id == currentRoundId) {
                it.copy(actions = it.actions.plus(action))
            } else {
                it
            }
        }
    )
}
