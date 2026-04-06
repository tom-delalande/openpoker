package domain.table

import domain.model.Table
import domain.model.Table.Round
import domain.model.Tournament
import java.time.Instant
import domain.model.Table.Round.Action.PlayerAction.RequestAction.ActionOption as ActionOption
import domain.model.Table.Round.Action.PlayerAction.*
import kotlin.collections.indexOf
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

fun create(tournamentDetails: Tournament.Details) {
    val table = Table(
        gameType = Table.GameType.HoldEm,
        betLimit = Table.BetLimit(
            betType = Table.BetLimit.BetType.NoLimit,
            betCap = 10.0,
        ),
        tableSize = tournamentDetails.players.size,
        dealerSeat = 0,
        smallBlindAmount = 2.0,
        bigBlindAmount = 4.0,
        anteAmount = 6.0,
        players = tournamentDetails.players.shuffled().mapIndexed { index, player ->
            Table.Player(
                id = player.id,
                name = player.name,
                seat = index,
                startingStack = tournamentDetails.startingStack,
                isSittingOut = false,
            )
        },
        rounds = listOf(
            Table.Round(
                id = 0,
                street = Table.Round.Street.PreFlop,
                cards = emptyList(),
                actions = emptyList(),
            )
        ),
        pots = listOf(),
        seed = Random.nextLong(),
    )
        .dealInitialCards()
}

private fun Table.dealInitialCards(): Table {
    val cards = getCards(players.size * 2)
    return players.foldIndexed(this) { index, table, player ->
        table.appendAction(
            DealCards(
                playerId = player.id,
                cards = cards.subList(index, index + 1)
            )
        )
    }
}

private fun Table.processTable(now: Instant): Table {
    return when (val latestAction = currentRound.actions.last()) {
        is RequestAction -> {
            if (latestAction.expiry.isBefore(now)) {
                this
                    .timeoutCurrentActionRequest(latestAction)
                    .requestNextAction(now)
            }
            this
        }

        else -> {
            this
                .attemptFinishRound()
                .requestNextAction(now)
        }
    }
}

private fun Table.getCards(numberOfCards: Int): List<Table.Card> {
    return Table.Card.Suit.entries.toList().flatMap { suit ->
        (1..13).map { rank ->
            Table.Card(suit, rank)
        }
    }.shuffled(Random(seed)).subList(currentNumberOfCards, currentNumberOfCards + numberOfCards)
}

private fun Table.attemptFinishRound(): Table {
    if (currentRound.street == Round.Street.Showdown) {
        // Do Showdown stuff here
    }
    val lastAction = playerActions.last()
    val lastActionPlayer = players.find { it.id == lastAction.playerId }
    val lastPlayerToRaise = playerActions.findLast { it is Raise }

    val nextPlayer = players[players.indexOf(lastActionPlayer) + 1 % players.size]
    if (lastPlayerToRaise == nextPlayer) {
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
                        actions = listOf()
                    )
                )
            )
        }
    }
    return this
}

private fun Table.requestNextAction(now: Instant): Table {
    val lastAction = playerActions.last()
    val lastActionPlayer = players.find { it.id == lastAction.playerId }

    val nextPlayer = players.get(players.indexOf(lastActionPlayer) + 1 % players.size)

    val playerRaise = currentRaiseByPlayer[nextPlayer.id]!!
    val playerStack = currentStackByPlayer[nextPlayer.id]!!

    val actionOptions = buildList {
        if (currentRound.street == Round.Street.PreFlop && nextPlayer.id == smallBlindPlayer.id) {
            add(ActionOption.Fold)
            add(ActionOption.PostSmallBlind(amount = min(smallBlindAmount, playerStack)))
            return@buildList
        }

        if (currentRound.street == Round.Street.PreFlop && nextPlayer.id == bigBlindPlayer.id) {
            add(ActionOption.Fold)
            add(ActionOption.PostBigBlind(amount = min(bigBlindAmount, playerStack)))
            return@buildList
        }

        if (playerStack < currentRaise) {
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
