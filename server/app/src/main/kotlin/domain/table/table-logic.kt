package domain.table

import app.logger
import domain.model.Table
import domain.model.Table.Round
import java.time.Instant
import domain.model.Table.Round.Action.PlayerAction.RequestAction.ActionOption as ActionOption
import domain.model.Table.Round.Action.PlayerAction.*
import domain.model.shift
import domain.tournament.CashGameRepository
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import server.PlayerActionRequest
import server.models.BetOption

const val DEFAULT_SMALL_BLIND_AMOUNT = 5.0
const val DEFAULT_BIG_BLIND_AMOUNT = 10.0
const val DEFAULT_ANTE_AMOUNT = 20.0
const val DEFAULT_TIMEOUT_IN_SECONDS = 10L

fun createTable(
    players: List<CashGameRepository.Player> = listOf(),
    smallBlindAmount: Double = DEFAULT_SMALL_BLIND_AMOUNT,
    bigBlindAmount: Double = DEFAULT_BIG_BLIND_AMOUNT,
    anteAmount: Double = DEFAULT_ANTE_AMOUNT,
    seed: Long = Random.nextLong(),
): Table {
    val table = Table(
        gameType = Table.GameType.HoldEm,
        // TODO: [medium] this is unused
        betLimit = Table.BetLimit(
            betType = Table.BetLimit.BetType.NoLimit,
            betCap = null,
        ),
        tableSize = players.size,
        dealerSeat = 0,
        smallBlindAmount = smallBlindAmount,
        bigBlindAmount = bigBlindAmount,
        anteAmount = anteAmount,
        players = players.mapIndexed { index, player ->
            Table.Player(
                id = player.id,
                name = player.name,
                seat = index,
                stack = player.stack,
                isSittingOut = false,
            )
        },
        rounds = listOf(),
        pots = listOf(),
        seed = seed,
    )

    return table
}

fun Table.addPlayer(player: CashGameRepository.Player) = copy(
    players = players + Table.Player(
        id = player.id,
        name = player.name,
        seat = players.size,
        stack = player.stack,
        isSittingOut = false,
    ),
)

fun Table.dealInitialCards(): Table {
    return copy(
        rounds = listOf(
            Round(
                id = 0,
                street = Round.Street.PreFlop,
                actions = emptyList(),
            )
        )
    ).dealCards()
}

private fun Table.dealCards(): Table {
    val cards = getCards(players.size * 2)
    val firstSeat = smallBlindPlayer.seat
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

fun Table.processTable(now: Instant, seedGenerator: () -> Long = { Random.nextLong() }): Table {
    if (rounds.isEmpty() && players.size >= 3) {
        return startNextHand(dealerSeat = dealerSeat, seed = seedGenerator())
    }

    return when (val latestAction = currentRound?.actions?.lastOrNull()) {
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

fun Table.startNextHand(
    smallBlindAmount: Double = this.smallBlindAmount,
    bigBlindAmount: Double = this.bigBlindAmount,
    dealerSeat: Int = smallBlindPlayer.seat,
    seed: Long = Random.nextLong(),
): Table {
    return copy(
        dealerSeat = dealerSeat,
        players = players.map { player ->
            player.copy(
                isSittingOut = false,
                stack = livePlayers.first { it.playerId == player.id }.currentStack + pots.flatMap { it.playerWins }
                    .filter { it.playerId == player.id }
                    .sumOf { it.winAmount }
            )
        },
        smallBlindAmount = smallBlindAmount,
        bigBlindAmount = bigBlindAmount,
        isFinished = false,
        rounds = listOf(
            Round(
                id = 0,
                street = Round.Street.PreFlop,
                actions = listOf()
            )
        ),
        pots = listOf(),
        seed = seed,
    )
}

fun Table.processPostAction(now: Instant): Table = attemptFinishRound()
    .performDealerActions()
    .requestNextAction(now)

fun Table.performDealerActions(): Table {
    if (playerRoundActions.lastOrNull() is PostBigBlind) {
        return dealCards()
    }
    return this
}

fun Table.processPlayerAction(playerId: Int, action: PlayerActionRequest, now: Instant): Table {
    val lastAction = playerRoundActions.last()
    if (lastAction !is RequestAction) {
        throw IllegalStateException("Unexpected action. playerId[$playerId] action[$action] lastEvent[$lastAction]")
    }
    if (lastAction.playerId != playerId) {
        throw IllegalStateException("Unexpected action for player. playerId[$playerId] expectedPlayerId[${lastAction.playerId}]")
    }

    val actionOptions = lastAction.actionOptions
    val player = livePlayers.first { it.playerId == playerId }

    val actionAllowed = when (action) {
        is PlayerActionRequest.Bet -> actionOptions.filterIsInstance<ActionOption.Bet>()
            .any { (action.amount >= it.minAmount && (it.maxAmount == null || action.amount <= it.maxAmount)) || player.currentStack == action.amount }

        is PlayerActionRequest.Call -> actionOptions.filterIsInstance<ActionOption.Call>()
            .any { action.amount == it.amount || player.currentStack == action.amount }

        is PlayerActionRequest.Check -> actionOptions.filterIsInstance<ActionOption.Check>().any()
        is PlayerActionRequest.Fold -> actionOptions.filterIsInstance<ActionOption.Fold>().any()
        is PlayerActionRequest.PostBigBlind -> actionOptions.filterIsInstance<ActionOption.PostBigBlind>()
            .any { action.amount == it.amount || player.currentStack == action.amount }

        is PlayerActionRequest.PostSmallBlind -> actionOptions.filterIsInstance<ActionOption.PostSmallBlind>()
            .any { action.amount == it.amount || player.currentStack == action.amount }

        is PlayerActionRequest.Raise -> actionOptions.filterIsInstance<ActionOption.Raise>()
            .any { (action.amount >= it.minAmount && (it.maxAmount == null || action.amount <= it.maxAmount)) || player.currentStack == action.amount }

        is PlayerActionRequest.MuckCards -> actionOptions.filterIsInstance<ActionOption.MuckCards>().any()
        is PlayerActionRequest.ShowCards -> actionOptions.filterIsInstance<ActionOption.ShowCards>()
            .any { action.cards == livePlayers.find { it.playerId == action.playerId }?.cards }

        is PlayerActionRequest.PostDeadBlind -> TODO()
        is PlayerActionRequest.PostExtraBlind -> TODO()
        is PlayerActionRequest.PostStraddle -> TODO()
        is PlayerActionRequest.PostAnte -> TODO()
        is PlayerActionRequest.SitDown -> TODO()
        is PlayerActionRequest.StandUp -> TODO()
    }

    if (!actionAllowed) {
        throw IllegalStateException("Unexpected action. playerId[$playerId] action[$action] options[$actionOptions]")
    }

    val tableAction = when (action) {
        is PlayerActionRequest.Bet -> Bet(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.currentStack == action.amount,
        )

        is PlayerActionRequest.Call -> Call(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.currentStack == action.amount,
        )

        is PlayerActionRequest.Check -> Check(
            playerId = action.playerId,
        )

        is PlayerActionRequest.Fold -> Fold(
            playerId = action.playerId,
        )

        is PlayerActionRequest.MuckCards -> MuckCards(
            playerId = action.playerId,
            cards = listOf(),
        )

        is PlayerActionRequest.PostAnte -> PostAnte(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.currentStack == action.amount,
        )

        is PlayerActionRequest.PostBigBlind -> PostBigBlind(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.currentStack == action.amount,
        )

        is PlayerActionRequest.PostDeadBlind -> PostDeadBlind(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.currentStack == action.amount,
        )

        is PlayerActionRequest.PostExtraBlind -> PostExtraBlind(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.currentStack == action.amount,
        )

        is PlayerActionRequest.PostSmallBlind -> PostSmallBlind(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.currentStack == action.amount,
        )

        is PlayerActionRequest.PostStraddle -> PostStraddle(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.currentStack == action.amount,
        )

        is PlayerActionRequest.Raise -> Raise(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.currentStack == action.amount,
        )

        is PlayerActionRequest.ShowCards -> ShowCards(
            playerId = action.playerId,
            cards = player.pocketCards,
        )

        is PlayerActionRequest.SitDown -> SitDown(
            playerId = action.playerId,
            playerName = player.name,
            seat = player.seat,
            stack = player.currentStack,
        )

        is PlayerActionRequest.StandUp -> StandUp(
            playerId = action.playerId,
        )
    }

    return appendAction(tableAction)
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
    if (rounds.isEmpty() || playerRoundActions.isEmpty() || players.isEmpty()) {
        return copy()
    }

    if (livePlayers.count { !it.isOut } == 1) {
        return finishHand()
    }

    val lastAction = playerRoundActions.filterNot { it is RequestAction }.last()
    val lastActionPlayer = livePlayers.find { it.playerId == lastAction.playerId }
    val nextPlayerToAct = lastActionPlayer?.nextPlayerToAct()
    val lastRaiseActionPlayerId =
        if (currentRound?.street != Round.Street.PreFlop && playerRoundActions.none { it is Raise || it is Bet }) {
            playerRoundActions.firstOrNull { it is Check }?.playerId
        } else if (currentRound?.street == Round.Street.PreFlop && playerRoundActions.any { it !is PostSmallBlind && it !is PostBigBlind && it !is RequestAction && it !is Fold }) {
            playerRoundActions.dropLast(1).findLast { it is Raise || it is Bet }?.playerId
            // TODO: [low] add  test to check if this breaks when UTG folds
                ?: players.find { it.seat == bigBlindPlayer.seat.nextSeat() }?.id
        } else {
            playerRoundActions.dropLast(1).findLast { it is Raise || it is Bet }?.playerId
        }

    if (lastRaiseActionPlayerId == nextPlayerToAct?.playerId) {
        if (currentRound?.street == Round.Street.River || livePlayers.all { it.isAllIn }) {
            return finishHand()
        } else {
            val street = when (currentRound?.street) {
                Round.Street.PreFlop -> Round.Street.Flop
                Round.Street.Flop -> Round.Street.Turn
                Round.Street.Turn -> Round.Street.River
                Round.Street.River,
                Round.Street.Showdown,
                null,
                    -> throw IllegalStateException("Cannot go to next round, you should have finished hand.")
            }
            val cards = when (street) {
                Round.Street.PreFlop -> emptyList()
                Round.Street.Flop -> getCards(3)
                Round.Street.Turn -> getCards(1)
                Round.Street.River -> getCards(1)
                Round.Street.Showdown -> emptyList()
            }
            return copy(
                rounds = rounds.plus(
                    Round(
                        id = currentRound!!.id + 1,
                        street = street,
                        actions = listOf(
                            Round.Action.DealCommunityCards(cards)
                        )
                    )
                )
            )
        }
    }
    return this
}

private fun Table.finishHand(): Table {
    val winners = calculateWinners()
    val winnings = pot / winners.count()

    val firstPlayerToShow = rounds
        .flatMap { it.actions }
        .filterIsInstance<Round.Action.PlayerAction>()
        .lastOrNull { it is Bet || it is Raise }
        ?.playerId ?: smallBlindPlayer.id

    val firstSeatToShow = livePlayers.find { it.playerId == firstPlayerToShow }?.seat ?: smallBlindPlayer.seat
    val players = livePlayers.sortedBy { it.seat }.shift(firstSeatToShow).filterNot { it.isOut }

    val showdown = if (players.size > 1) {
        listOf(
            Round(
                id = 4,
                street = Round.Street.Showdown,
                actions = players.map {
                    ShowCards(it.playerId, it.pocketCards)
                }
            )
        )
    } else emptyList()

    // TODO: [high] Calculate split pots and all that
    return copy(
        isFinished = true,
        rounds = rounds + showdown,
        pots = listOf(
            Table.Pot(
                number = 0,
                amount = pot,
                jackpot = 0.0,
                playerWins = winners.map {
                    Table.Pot.PlayerWin(
                        playerId = it,
                        winAmount = winnings,
                    )
                }
            ))
    )
}

private fun Table.calculateWinners(): List<Int> {
    val handRatings = livePlayers.filterNot { it.isOut }.associateWith { (it.cards).rateHand().score }
    val winningRating = handRatings.maxOf { it.value }
    return handRatings.filterValues { it == winningRating }.map { it.key.playerId }
}

private fun Table.requestNextAction(now: Instant): Table {
    if (rounds.isEmpty() || players.isEmpty() || isFinished) return copy()

    val playerRaise = nextPlayerToAct.contributionThisStreet
    val playerStack = nextPlayerToAct.currentStack

    val actionOptions = buildList {
        if (currentRound?.street == Round.Street.PreFlop
            && nextPlayerToAct.playerId == smallBlindPlayer.id && playerRoundActions.filterIsInstance<PostSmallBlind>()
                .none()
        ) {
            add(ActionOption.PostSmallBlind(amount = min(smallBlindAmount, playerStack)))
            return@buildList
        }

        if (currentRound?.street == Round.Street.PreFlop && nextPlayerToAct.playerId == bigBlindPlayer.id && playerRoundActions.filterIsInstance<PostBigBlind>()
                .none()
        ) {
            add(ActionOption.PostBigBlind(amount = min(bigBlindAmount, playerStack)))
            return@buildList
        }

        if (playerRaise <= currentRaise) {
            add(ActionOption.Fold)
        }

        if (playerRaise >= currentRaise) {
            add(ActionOption.Check)
        }

        if (currentRaise > 0.0 && playerRaise < currentRaise && playerStack > 0.0) {
            add(ActionOption.Call(amount = min(playerStack, currentRaise - playerRaise)))
        }

        if (currentRaise == 0.0 && playerStack > 0.0) {
            add(ActionOption.Bet(minAmount = bigBlindAmount, maxAmount = playerStack))
        }

        if (currentRaise > 0.0 && playerStack > 0.0) {
            add(
                // TODO: [medium] raises might still be wrong
                ActionOption.Raise(
                    minAmount = (currentRaise - previousRaise) + bigBlindAmount,
                    maxAmount = max(playerRaise - currentRaise, playerStack)
                )
            )
        }

    }


    logger.info("Request actions. playerId[${nextPlayerToAct.playerId}] options[${actionOptions}]")
    val actionRequest = RequestAction(
        playerId = nextPlayerToAct.playerId,
        actionOptions = actionOptions,
        expiry = now.plusSeconds(10),
    )
    return appendAction(actionRequest)
}

private fun Table.timeoutCurrentActionRequest(latestAction: RequestAction): Table {
    logger.info("Timeout. playerId[${latestAction.playerId}]")
    val defaultAction = latestAction.actionOptions.first()
    val playerId = latestAction.playerId
    val player = livePlayers.first { it.playerId == playerId }

    val newAction = when (defaultAction) {
        ActionOption.Check -> Check(
            playerId = playerId,
        )

        ActionOption.Fold -> Fold(
            playerId = playerId,
        )

        is ActionOption.PostSmallBlind -> PostSmallBlind(
            playerId = playerId,
            amount = min(player.currentStack, defaultAction.amount),
            isAllIn = defaultAction.amount >= player.currentStack
        )

        is ActionOption.PostBigBlind -> PostBigBlind(
            playerId = playerId,
            amount = min(player.currentStack, defaultAction.amount),
            isAllIn = defaultAction.amount >= player.currentStack
        )

        is ActionOption.Call,
        is ActionOption.Bet,
        is ActionOption.PostAnte,
        is ActionOption.PostDeadBlind,
        is ActionOption.PostExtraBlind,
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
