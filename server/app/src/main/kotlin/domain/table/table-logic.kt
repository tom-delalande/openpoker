package domain.table

import app.logger
import domain.model.Table
import domain.model.Table.Round
import java.time.Instant
import domain.model.Table.Round.Action.PlayerAction.RequestAction.ActionOption as ActionOption
import domain.model.Table.Round.Action.PlayerAction.*
import domain.model.shift
import domain.tournament.CashGameRepository
import java.util.UUID
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import server.PlayerActionRequest

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
        handId = UUID.randomUUID(),
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
        rounds = listOf(
            Round(
                id = 0,
                street = Round.Street.PreFlop,
                actions = players.mapIndexed { index, it ->
                    SitDown(
                        playerId = it.id,
                        playerName = it.name,
                        seat = index,
                        stack = it.stack,
                    )
                }
            )
        ),
        pots = listOf(),
        seed = seed,
    )

    return table
}

fun Table.addPlayer(player: CashGameRepository.Player): Table {
    return if (livePlayers.any { it.playerId == player.id }) {
        copy()
    } else {
        appendAction(SitDown(player.id, player.name, livePlayers.size, player.stack))
    }
}

private fun Table.dealCards(): Table {
    val cards = getCards(livePlayers.size * 2)
    val firstSeat = smallBlindPlayer.seat
    val orderedPLayers = (firstSeat..<firstSeat + livePlayers.size).map { it % livePlayers.size }
    return orderedPLayers.fold(this) { table, seat ->
        val player = livePlayers[seat]
        table.appendAction(
            DealCards(
                playerId = player.playerId,
                cards = cards.subList(2 * seat, 2 * seat + 2)
            )
        )
    }
}

fun Table.nextHandPlayers() = livePlayers
    .map { player ->
        player.copy(
            isSittingOut = false,
            stack = player.stack + pots.flatMap { it.playerWins }
                .filter { it.playerId == player.playerId }
                .sumOf { it.winAmount }
        )
    }
    .filter { player -> player.stack > 0 }

fun Table.processTable(now: Instant, seedGenerator: () -> Long = { Random.nextLong() }): Table {
    if (isFinished) return this

    if (livePlayers.count { !it.isSittingOut } < 2) {
        return finishHand(now)
    }

    if (!isStarted && livePlayers.size >= 3) {
        return startNextHand(dealerSeat = dealerSeat, seed = seedGenerator(), now = now)
    }

    return when (val latestAction = currentRound?.actions?.lastOrNull()) {
        is RequestAction -> {
            if (latestAction.expiry.isBefore(now) || livePlayers.find { it.playerId == latestAction.playerId }?.isSittingOut == true) {
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
    now: Instant,
): Table {
    if (nextHandPlayers().size <= 1) {
        return copy()
    }
    return copy(
        handId = UUID.randomUUID(),
        dealerSeat = dealerSeat,
        isStarted = true,
        startedAt = now,
        smallBlindAmount = smallBlindAmount,
        bigBlindAmount = bigBlindAmount,
        isFinished = false,
        finishedAt = null,
        rounds = listOf(
            Round(
                id = 0,
                street = Round.Street.PreFlop,
                actions = listOf(
                    *rounds.first().actions.toTypedArray(),
                    *nextHandPlayers().map {
                        SitDown(
                            playerId = it.playerId,
                            playerName = it.name,
                            stack = it.stack,
                            seat = it.seat,
                        )
                    }.toTypedArray(),
                    Round.Action.HandStarted,
                    Round.Action.RoundStarted(
                        id = 0,
                        street = Round.Street.PreFlop,
                    ),
                )
            )
        ),
        pots = listOf(),
        seed = seed,
    ).autoPostBlinds()
}

fun Table.autoPostBlinds() = copy(
    rounds = rounds.map {
        if (it.street == Round.Street.PreFlop) it.copy(
            actions = it.actions + listOf(
                PostSmallBlind(
                    smallBlindPlayer.playerId,
                    min(smallBlindAmount, smallBlindPlayer.stack),
                    smallBlindAmount >= smallBlindPlayer.stack
                ),
                PostBigBlind(
                    bigBlindPlayer.playerId,
                    min(bigBlindAmount, bigBlindPlayer.stack),
                    bigBlindAmount >= bigBlindPlayer.stack
                )
            )
        ) else it
    }
)

fun Table.processPostAction(now: Instant): Table = attemptFinishRound(now)
    .performDealerActions()
    .requestNextAction(now)

fun Table.performDealerActions(): Table {
    if (playerRoundActions.lastOrNull() is PostBigBlind) {
        return dealCards()
    }
    return this
}

fun Table.processPlayerAction(playerId: Int, action: PlayerActionRequest, now: Instant): Table {
    when (action) {
        is PlayerActionRequest.StandUp -> return appendAction(StandUp(playerId))
        else -> {}
    }

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
            .any { (action.amount >= it.minAmount && (it.maxAmount == null || action.amount <= it.maxAmount)) || player.stack == action.amount }

        is PlayerActionRequest.Call -> actionOptions.filterIsInstance<ActionOption.Call>()
            .any { action.amount == it.amount || player.stack == action.amount }

        is PlayerActionRequest.Check -> actionOptions.filterIsInstance<ActionOption.Check>().any()
        is PlayerActionRequest.Fold -> actionOptions.filterIsInstance<ActionOption.Fold>().any()
        is PlayerActionRequest.PostBigBlind -> actionOptions.filterIsInstance<ActionOption.PostBigBlind>()
            .any { action.amount == it.amount || player.stack == action.amount }

        is PlayerActionRequest.PostSmallBlind -> actionOptions.filterIsInstance<ActionOption.PostSmallBlind>()
            .any { action.amount == it.amount || player.stack == action.amount }

        is PlayerActionRequest.Raise -> actionOptions.filterIsInstance<ActionOption.Raise>()
            .any { (action.amount >= it.minAmount && (it.maxAmount == null || action.amount <= it.maxAmount)) || player.stack == action.amount }

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
            isAllIn = player.stack == action.amount,
        )

        is PlayerActionRequest.Call -> Call(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.stack == action.amount,
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
            isAllIn = player.stack == action.amount,
        )

        is PlayerActionRequest.PostBigBlind -> PostBigBlind(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.stack == action.amount,
        )

        is PlayerActionRequest.PostDeadBlind -> PostDeadBlind(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.stack == action.amount,
        )

        is PlayerActionRequest.PostExtraBlind -> PostExtraBlind(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.stack == action.amount,
        )

        is PlayerActionRequest.PostSmallBlind -> PostSmallBlind(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.stack == action.amount,
        )

        is PlayerActionRequest.PostStraddle -> PostStraddle(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.stack == action.amount,
        )

        is PlayerActionRequest.Raise -> Raise(
            playerId = action.playerId,
            amount = action.amount,
            isAllIn = player.stack == action.amount,
        )

        is PlayerActionRequest.ShowCards -> ShowCards(
            playerId = action.playerId,
            cards = player.pocketCards,
        )

        is PlayerActionRequest.SitDown -> SitDown(
            playerId = action.playerId,
            playerName = player.name,
            seat = player.seat,
            stack = player.stack,
        )

        is PlayerActionRequest.StandUp -> StandUp(
            playerId = action.playerId,
        )
    }

    return appendAction(tableAction)
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

private fun Table.attemptFinishRound(now: Instant): Table {
    if (!isStarted || playerRoundActions.isEmpty() || livePlayers.isEmpty()) {
        return copy()
    }

    if (livePlayers.count { !it.isOut && !it.isSittingOut } <= 1) {
        return finishHand(now)
    }

    if (livePlayers.all { it.isOut || it.isAllIn }) {
        val additionalRounds = buildList {
            val cards = getCards(5)
            if (currentRound!!.id < 1) {
                add(
                    Round(
                        1,
                        Round.Street.Flop,
                        actions = listOf(
                            Round.Action.RoundStarted(
                                id = 1,
                                street = Round.Street.Flop,
                            ),
                            Round.Action.DealCommunityCards(
                                cards = cards.subList(0, 3)
                            )
                        )
                    )
                )
            }
            if (currentRound!!.id < 2) {
                add(
                    Round(
                        2,
                        Round.Street.Turn,
                        actions = listOf(
                            Round.Action.RoundStarted(
                                id = 2,
                                street = Round.Street.Turn,
                            ),
                            Round.Action.DealCommunityCards(
                                cards = cards.subList(3, 5)
                            )
                        )
                    )
                )
            }
            if (currentRound!!.id < 3) {
                add(
                    Round(
                        3,
                        Round.Street.River,
                        actions = listOf(
                            Round.Action.RoundStarted(
                                id = 3,
                                street = Round.Street.River,
                            ),
                            Round.Action.DealCommunityCards(
                                cards = cards.subList(5, 5)
                            )
                        )
                    )
                )
            }
        }
        return copy(rounds = rounds + additionalRounds).finishHand(now)
    }

    val lastAction = playerRoundActions.filterNot { it is RequestAction }.last()
    val lastActionPlayer = livePlayers.find { it.playerId == lastAction.playerId }
    val nextPlayerToAct = lastActionPlayer?.nextPlayerToAct()
    val lastRaiseActionPlayerId =
        if (currentRound?.street != Round.Street.PreFlop && playerRoundActions.none { it is Raise || it is Bet }) {
            playerRoundActions.firstOrNull { it is Check }?.playerId
        } else if (currentRound?.street == Round.Street.PreFlop && playerRoundActions.any { it.playerId == bigBlindPlayer.nextPlayerToAct().playerId && (it is Check || it is Fold || it is Raise || it is Bet || it is Call) }) {
            playerRoundActions.dropLast(1).findLast { it is Raise || it is Bet }?.playerId
            // TODO: [low] add  test to check if this breaks when UTG folds
                ?: livePlayers.find { it.seat == bigBlindPlayer.seat.nextSeat() }?.playerId
        } else {
            playerRoundActions.dropLast(1).findLast { it is Raise || it is Bet }?.playerId
        }

    if (lastRaiseActionPlayerId == nextPlayerToAct?.playerId) {
        if (currentRound?.street == Round.Street.River || livePlayers.all { it.isAllIn }) {
            return finishHand(now)
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
                            Round.Action.RoundStarted(
                                id = currentRound!!.id + 1,
                                street = street,
                            ),
                            Round.Action.DealCommunityCards(cards)
                        )
                    )
                )
            )
        }
    }
    return this
}

private fun Table.finishHand(now: Instant): Table {
    if (isFinished) return this
    val winners = calculateWinners()
    if (winners.isEmpty()) return copy(
        isFinished = true,
        finishedAt = now,
        rounds = rounds.mapIndexed { index, it ->
            if (index == rounds.size - 1) (it.copy(
                actions = it.actions + listOf(Round.Action.HandEnded)
            )) else it
        },
        pots = pots,
    )
    val winnings = pot / winners.count()


    val firstPlayerToShow = rounds
        .flatMap { it.actions }
        .filterIsInstance<Round.Action.PlayerAction>()
        .lastOrNull { it is Bet || it is Raise }
        ?.playerId ?: smallBlindPlayer.playerId

    val firstSeatToShow = livePlayers.find { it.playerId == firstPlayerToShow }?.seat ?: smallBlindPlayer.seat
    val players = livePlayers.sortedBy { it.seat }.shift(firstSeatToShow).filterNot { it.isOut }

    val pots = listOf(
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
        )
    )

    val outPlayers = copy(pots = pots).livePlayers.map { player ->
        player.copy(
            stack = player.stack + pots.flatMap { it.playerWins }
                .filter { it.playerId == player.playerId }
                .sumOf { it.winAmount }
        )
    }.filter { it.stack == 0.0 }

    val showdown = if (players.size > 1) {
        listOf(
            Round(
                id = 4,
                street = Round.Street.Showdown,
                actions = players.map {
                    ShowCards(it.playerId, it.pocketCards)
                } + outPlayers.map { StandUp(it.playerId) }
            )
        )
    } else emptyList()

    // TODO: [high] Calculate split pots and all that
    return copy(
        isFinished = true,
        finishedAt = now,
        rounds = (rounds + showdown).mapIndexed { index, it ->
            if (index == (rounds + showdown).size - 1) (it.copy(
                actions = it.actions + listOf(Round.Action.HandEnded)
            )) else it
        },
        pots = pots,
    )
}

private fun Table.calculateWinners(): List<Int> {
    val handRatings =
        livePlayers.filterNot { it.isOut && !it.isSittingOut }.associateWith { (it.cards).rateHand().score }
    val winningRating = handRatings.maxOf { it.value }
    return handRatings.filterValues { it == winningRating }.map { it.key.playerId }
}

private fun Table.requestNextAction(now: Instant): Table {
    if (!isStarted || livePlayers.isEmpty() || isFinished) return copy()

    val playerRaise = nextPlayerToAct.contributionThisStreet
    val playerStack = nextPlayerToAct.stack

    val actionOptions = buildList {
        if (currentRound?.street == Round.Street.PreFlop
            && nextPlayerToAct.playerId == smallBlindPlayer.playerId && playerRoundActions.filterIsInstance<PostSmallBlind>()
                .none()
        ) {
            add(ActionOption.PostSmallBlind(amount = min(smallBlindAmount, playerStack)))
            return@buildList
        }

        if (currentRound?.street == Round.Street.PreFlop && nextPlayerToAct.playerId == bigBlindPlayer.playerId && playerRoundActions.filterIsInstance<PostBigBlind>()
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
            amount = min(player.stack, defaultAction.amount),
            isAllIn = defaultAction.amount >= player.stack
        )

        is ActionOption.PostBigBlind -> PostBigBlind(
            playerId = playerId,
            amount = min(player.stack, defaultAction.amount),
            isAllIn = defaultAction.amount >= player.stack
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
    logger.info("Auto-acting for player. playerId[$playerId] action[$newAction]")

    return appendAction(newAction)
        .appendAction(StandUp(playerId))
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
