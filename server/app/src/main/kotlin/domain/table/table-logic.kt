package domain.table

import app.logger
import domain.model.Table
import domain.model.Table.Round
import java.time.Instant
import domain.model.Table.Round.Action.PlayerAction.RequestAction.ActionOption as ActionOption
import domain.model.Table.Round.Action.PlayerAction.*
import domain.model.shiftSeat
import domain.tournament.CashGameRepository
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import server.PlayerActionRequest

const val DEFAULT_SMALL_BLIND_AMOUNT = 5.0
const val DEFAULT_BIG_BLIND_AMOUNT = 10.0
const val DEFAULT_ANTE_AMOUNT = 20.0

fun createTable(
    players: List<CashGameRepository.Player> = listOf(),
    smallBlindAmount: Double = DEFAULT_SMALL_BLIND_AMOUNT,
    bigBlindAmount: Double = DEFAULT_BIG_BLIND_AMOUNT,
    anteAmount: Double = DEFAULT_ANTE_AMOUNT,
    minPlayers: Int = 2,
    maxPlayers: Int = 3,
    seed: Long = Random.nextLong(),
    defaultCards: List<Table.Card> = emptyList(),
): Table {
    val table = Table(
        gameType = Table.GameType.HoldEm,
        betLimit = Table.BetLimit(
            betType = Table.BetLimit.BetType.NoLimit,
            betCap = null,
        ),
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
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
        defaultCards = defaultCards,
        seed = seed,
    )

    return table
}

fun Table.addPlayer(player: CashGameRepository.Player): Table {
    return if (players.any { it.playerId == player.id && !it.isSittingOut }) {
        copy()
    } else {
        appendAction(
            SitDown(
                player.id,
                player.name,
                (0..9).first { !players.map { it.seat }.contains(it) },
                player.stack
            )
        )
    }
}

private fun Table.dealCards(): Table {
    val cards = getCards(activePlayers.size * 2)
    val orderedPlayers = activePlayers.shiftSeat(dealerSeat + 1)
    return orderedPlayers.foldIndexed(this) { index, table, player ->
        table.appendAction(
            DealCards(
                playerId = player.playerId,
                cards = cards.subList(2 * index, 2 * index + 2)
            )
        )
    }
}

fun Table.nextHandPlayers() = players.filterNot { it.isSittingOut }
    .mapIndexed { index, player ->
        player.copy(
            isSittingOut = false,
            seat = index,
            isNew = false,
            stack = player.stack
        )
    }

fun Table.processTable(now: Instant, seedGenerator: () -> Long = { Random.nextLong() }): Table {
    if (!isFinished && isStarted && players.count { !it.isSittingOut } < 2) {
        return finishHand(now)
    }

    if (!isStarted && players.filterNot { it.isSittingOut }.size >= minPlayers) {
        return startNextHand(dealerSeat = dealerSeat, seed = seedGenerator(), now = now, includePreFlopEvents = true)
    }
    if (isFinished && finishedAt?.plusSeconds(5)?.isBefore(now) == true && activePlayers.size > 1) {
        return startNextHand(dealerSeat = dealerSeat.nextSeat(), seed = seedGenerator(), now = now)
    }

    if (isFinished) return this
    return when (val latestAction = currentRound?.actions?.lastOrNull()) {
        is RequestAction -> {
            if (latestAction.expiry.isBefore(now) || players.find { it.playerId == latestAction.playerId }?.isSittingOut == true) {
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
    dealerSeat: Int = this.dealerSeat + 1,
    includePreFlopEvents: Boolean = false,
    seed: Long = Random.nextLong(),
    now: Instant,
): Table {
    val (outPlayers, inPlayers) = nextHandPlayers().partition { it.stack == 0.0 }
    if (inPlayers.size <= 1) {
        return copy()
    }
    return copy(
        handVersion = handVersion + 1,
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
                    *(if (includePreFlopEvents) rounds.first().actions.toTypedArray() else emptyArray()),
                    *outPlayers.map { StandUp(playerId = it.playerId, stack = 0.0) }.toTypedArray(),
                    *inPlayers.map {
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
            actions = it.actions + listOfNotNull(
                smallBlindPlayer?.let { smallBlindPlayer ->
                    PostSmallBlind(
                        smallBlindPlayer.playerId,
                        min(smallBlindAmount, smallBlindPlayer.stack),
                        smallBlindAmount >= smallBlindPlayer.stack
                    )
                },
                bigBlindPlayer?.let { bigBlindPlayer ->
                    PostBigBlind(
                        bigBlindPlayer.playerId,
                        min(bigBlindAmount, bigBlindPlayer.stack),
                        bigBlindAmount >= bigBlindPlayer.stack
                    )
                },
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
        is PlayerActionRequest.StandUp -> return if (players.first { it.playerId == playerId }.isAllIn) {
            copy()
        } else {
            appendAction(
                StandUp(
                    playerId,
                    players.first { it.playerId == playerId }.stack
                )
            )

        }

        else -> {}
    }

    val lastAction = playerRoundActions.last()
    if (lastAction !is RequestAction) {
        throw IllegalStateException("Unexpected action. playerId[$playerId] action[$action] lastEvent[$lastAction]")
    }
    if (lastAction.playerId != playerId) {
        throw IllegalStateException("Unexpected action for player. playerId[$playerId] expectedPlayerId[${lastAction.playerId}] action[$action]")
    }

    val actionOptions = lastAction.actionOptions
    val player = players.first { it.playerId == playerId && !it.isSittingOut }

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
            .any { action.cards == players.find { it.playerId == action.playerId }?.cards }

        is PlayerActionRequest.PostDeadBlind -> TODO()
        is PlayerActionRequest.PostExtraBlind -> TODO()
        is PlayerActionRequest.PostStraddle -> TODO()
        is PlayerActionRequest.PostAnte -> TODO()
        is PlayerActionRequest.SitDown -> TODO()
        is PlayerActionRequest.StandUp -> TODO()
    }

    if (!actionAllowed) {
        throw IllegalStateException("Unallowed action. playerId[$playerId] action[$action] options[$actionOptions]")
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
            stack = player.stack,
        )
    }

    return appendAction(tableAction)
}

private fun Table.getCards(numberOfCards: Int): List<Table.Card> {
    return getCards(defaultCards, seed, currentNumberOfCards, numberOfCards)
}

fun getCards(defaultCards: List<Table.Card>, seed: Long, offset: Int, numberOfCards: Int): List<Table.Card> {
    if (defaultCards.size >= offset + numberOfCards) {
        return defaultCards.subList(offset, offset + numberOfCards)
    }
    return Table.Card.Suit.entries.toList().flatMap { suit ->
        (1..13).map { rank ->
            Table.Card(suit, rank)
        }
    }.shuffled(Random(seed)).subList(offset, offset + numberOfCards)
}

private fun Table.attemptFinishRound(now: Instant): Table {
    if (!isStarted || playerRoundActions.isEmpty() || players.isEmpty()) {
        return copy()
    }

    if (players.count { !it.isOut && !it.isSittingOut } <= 1) {
        return finishHand(now)
    }

    val onlyRemainingPlayer = players.singleOrNull {
        !it.isAllIn &&
                !it.isOut &&
                !it.isSittingOut
    }

    val lastRaiseActionPlayerId =
        if (currentRound?.street != Round.Street.PreFlop && playerRoundActions.none { it is Raise || it is Bet }) {
            playerRoundActions.firstOrNull { it is Check }?.playerId
        } else if (
        // This is completely overcomplicated but if the round is pre-flop
        // The last decisive action is the first player performs an action - because Small Blind and Big Blind
            currentRound?.street == Round.Street.PreFlop &&
            playerRoundActions.any { (it is Check || it is Fold || it is Raise || it is Bet || it is Call) }
        ) {
            playerRoundActions.findLast { it is Raise || it is Bet }?.playerId
                ?: playerRoundActions.first { it is Check || it is Fold || it is Raise || it is Bet || it is Call }.playerId
        } else {
            playerRoundActions.dropLast(1).findLast { it is Raise || it is Bet }?.playerId
        }

    val lastRaiseActionPlayer = players.find { it.playerId == lastRaiseActionPlayerId }

    if (players.all { it.isOut || it.isAllIn } || onlyRemainingPlayer != null && (onlyRemainingPlayer == lastRaiseActionPlayer || onlyRemainingPlayer == lastActivePlayerToAct)) {
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
                                cards = cards.subList(3, 4)
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
                                cards = cards.subList(4, 5)
                            )
                        )
                    )
                )
            }
        }
        return copy(rounds = rounds + additionalRounds).finishHand(now)
    }


    val nextPlayerToAct = if (lastRaiseActionPlayer?.isAllIn == true) {
        lastActivePlayerToAct?.nextPlayerToAct(false)
    } else {
        nextPlayerToAct
    }

    if (lastRaiseActionPlayerId == nextPlayerToAct?.playerId) {
        if (currentRound?.street == Round.Street.River || players.all { it.isAllIn }) {
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

    val handRatings = calculateSortedHandRatings()
    if (handRatings.isEmpty()) return copy(
        isFinished = true,
        finishedAt = now,
        rounds = rounds.mapIndexed { index, it ->
            if (index == rounds.size - 1) (it.copy(
                actions = it.actions + listOf(Round.Action.HandEnded(players.map {
                    Round.Action.PlayerStack(
                        it.playerId,
                        it.initialStack
                    )
                }))
            )) else it
        },
        pots = pots,
    )

    val firstPlayerToShow = rounds
        .flatMap { it.actions }
        .filterIsInstance<Round.Action.PlayerAction>()
        .lastOrNull { (it is Bet || it is Raise) && activePlayers.any { player -> player.playerId != it.playerId } }
        ?.playerId?.let { playerId -> activePlayers.find { it.playerId == playerId } }
        ?: firstActivePlayerAfterDealer

    val allPlayers = players.shiftSeat(firstPlayerToShow.seat)
    val inPlayers =
        activePlayers.filterNot { it.isOut || it.isSittingOut }.shiftSeat(firstPlayerToShow.seat)

    val allContributions = players
        .map { player -> player to (player.initialStack - player.stack) }
        .sortedBy { it.second }

    val sidePots = mutableListOf<Table.Pot>()
    var accumulatedPot = 0.0
    var potNumber = 0

    for (i in allContributions.indices) {
        val currentContribution = allContributions[i].second
        if (currentContribution <= 0) continue

        val contributionDelta =
            if (i == 0) currentContribution else currentContribution - allContributions[i - 1].second

        val eligiblePlayers = players.filter { player ->
            (player.initialStack - player.stack) >= currentContribution
        }

        val potSize = contributionDelta * eligiblePlayers.size
        if (potSize > 0) {
            val winners = calculateWinnersFor(eligiblePlayers)
            val winAmount = potSize / winners.size

            sidePots.add(
                Table.Pot(
                    number = potNumber++,
                    amount = potSize,
                    jackpot = 0.0,
                    playerWins = winners.map { Table.Pot.PlayerWin(it, winAmount) }
                ))
            accumulatedPot += potSize
        }
    }

    val outPlayers = copy(pots = sidePots).players.map { player ->
        player.copy(
            stack = player.stack + sidePots.flatMap { it.playerWins }
                .filter { it.playerId == player.playerId }
                .sumOf { it.winAmount }
        )
    }.filter { it.stack == 0.0 }

    val playerStacks = allPlayers
        .map { player ->
            Round.Action.PlayerStack(
                playerId = player.playerId,
                stack = player.stack + sidePots.flatMap { it.playerWins }
                    .filter { it.playerId == player.playerId }
                    .sumOf { it.winAmount }
            )
        }

    val showdown = if (inPlayers.size > 1) {
        listOf(
            Round(
                id = 4,
                street = Round.Street.Showdown,
                actions = listOf(Round.Action.RoundStarted(id = 4, street = Round.Street.Showdown)) +
                        inPlayers.map { ShowCards(it.playerId, it.pocketCards) }
            )
        )
    } else emptyList()
    val updatedRounds = rounds + showdown

    return copy(
        isFinished = true,
        finishedAt = now,
        rounds = updatedRounds.mapIndexed { index, it ->
            if (index == updatedRounds.size - 1) (it.copy(
                actions = it.actions + listOf(Round.Action.HandEnded(playerStacks))
            )) else it
        },
        pots = sidePots,
    )
}

private fun Table.calculateWinnersFor(eligiblePlayers: List<Table.LivePlayerInfo>): List<Int> {
    val activeEligiblePlayers = eligiblePlayers.filterNot { it.isOut && !it.isSittingOut }
    val handRatings = activeEligiblePlayers.associateWith { it.cards.rateHand().score }
    val winningRating = handRatings.maxOfOrNull { it.value } ?: return emptyList()
    return handRatings.filterValues { it == winningRating }.map { it.key.playerId }
}

private fun Table.calculateSortedHandRatings(): Map<Table.LivePlayerInfo, Double> {
    val handRatings =
        players.filterNot { it.isOut && !it.isSittingOut }.associateWith { (it.cards).rateHand().score }
            .toSortedMap { (_, rating1), (_, rating2) -> rating2 - rating1 }
    return handRatings
}

private fun Table.requestNextAction(now: Instant): Table {
    if (!isStarted || players.isEmpty() || isFinished) return copy()

    val playerRaise = nextPlayerToAct.contributionThisStreet
    val playerStack = nextPlayerToAct.stack

    val actionOptions = buildList {
        if (currentRound?.street == Round.Street.PreFlop
            && nextPlayerToAct.playerId == smallBlindPlayer?.playerId && playerRoundActions.filterIsInstance<PostSmallBlind>()
                .none()
        ) {
            add(ActionOption.PostSmallBlind(amount = min(smallBlindAmount, playerStack)))
            return@buildList
        }

        if (currentRound?.street == Round.Street.PreFlop && nextPlayerToAct.playerId == bigBlindPlayer?.playerId && playerRoundActions.filterIsInstance<PostBigBlind>()
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
            add(ActionOption.Bet(minAmount = min(playerStack, bigBlindAmount), maxAmount = playerStack))
        }

        if (currentRaise > 0.0 && playerStack > 0.0 && (currentRaise - previousRaise) + bigBlindAmount > 0) {
            add(
                ActionOption.Raise(
                    minAmount = min((currentRaise - previousRaise) + bigBlindAmount, playerStack),
                    maxAmount = max(
                        playerRaise - currentRaise, playerStack
                    )
                )
            )
        }

    }

    logger.debug("Request actions. playerId[{}] options[{}]", nextPlayerToAct.playerId, actionOptions)
    val actionRequest = RequestAction(
        playerId = nextPlayerToAct.playerId,
        actionOptions = actionOptions,
        expiry = now.plusSeconds(timeoutDurationInSeconds),
    )
    return appendAction(actionRequest)
}

private fun Table.timeoutCurrentActionRequest(latestAction: RequestAction): Table {
    logger.debug("Timeout. playerId[${latestAction.playerId}]")
    val defaultAction = latestAction.actionOptions.first()
    val playerId = latestAction.playerId
    val player = players.first { it.playerId == playerId }

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
        .appendAction(StandUp(playerId, player.stack))
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
