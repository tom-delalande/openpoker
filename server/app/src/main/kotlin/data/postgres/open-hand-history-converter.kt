package data.postgres

import domain.model.Action
import domain.model.ActionType
import domain.model.GameType
import domain.model.OpenHandHistory
import domain.model.OpenHandHistoryDocument
import domain.model.Player
import domain.model.PlayerWin
import domain.model.Round
import domain.model.RoundStreet
import domain.model.Table
import domain.model.OPEN_HAND_HISTORY_SPEC_VERSION
import java.util.UUID

fun Table.toOpenHandHistory() = OpenHandHistoryDocument(
    ohh = OpenHandHistory(
        specVersion = OPEN_HAND_HISTORY_SPEC_VERSION,
        siteName = "OpenPoker",
        networkName = "OpenPoker",
        internalVersion = OPEN_HAND_HISTORY_SPEC_VERSION,
        tournament = false,
        tournamentInfo = null,
        gameNumber = UUID.randomUUID().toString(),
        startDateUtc = startedAt!!.toString(),
        tableName = "Table",
        tableHandle = null,
        tableSkin = null,
        gameType = GameType.HOLD_EM,
        betLimit = domain.model.BetLimit(
            betType = when (betLimit.betType) {
                Table.BetLimit.BetType.NoLimit -> domain.model.BetType.NO_LIMIT
            },
            betCap = betLimit.betCap ?: 0.0,
        ),
        tableSize = tableSize,
        currency = "USD",
        dealerSeat = dealerSeat,
        smallBlindAmount = smallBlindAmount,
        bigBlindAmount = bigBlindAmount,
        anteAmount = anteAmount,
        heroPlayerId = null,
        flags = null,
        players = players.filterNot { it.isSittingOut }.map { player ->
            Player(
                name = player.name,
                id = player.playerId,
                seat = player.seat,
                startingStack = player.stack,
                playerBounty = null,
                display = null,
                isSittingOut = player.isSittingOut,
            )
        },
        rounds = rounds.map { round ->
            Round(
                id = round.id,
                street = when (round.street) {
                    Table.Round.Street.PreFlop -> RoundStreet.PRE_FLOP
                    Table.Round.Street.Flop -> RoundStreet.FLOP
                    Table.Round.Street.Turn -> RoundStreet.TURN
                    Table.Round.Street.River -> RoundStreet.RIVER
                    Table.Round.Street.Showdown -> RoundStreet.SHOWDOWN
                },
                cards = round.actions.filterIsInstance<Table.Round.Action.DealCommunityCards>()
                    .firstOrNull()?.cards?.map { "${it.rank}${it.suit.name.first().lowercase()}" },
                actions = round.actions.filterIsInstance<Table.Round.Action.PlayerAction>()
                    .filter { it !is Table.Round.Action.PlayerAction.RequestAction }
                    .mapIndexed { index, action ->
                        Action(
                            actionNumber = index + 1,
                            playerId = action.playerId,
                            action = when (action) {
                                is Table.Round.Action.PlayerAction.DealCards -> ActionType.DEALT_CARDS
                                is Table.Round.Action.PlayerAction.MuckCards -> ActionType.MUCKS_CARDS
                                is Table.Round.Action.PlayerAction.ShowCards -> ActionType.SHOWS_CARDS
                                is Table.Round.Action.PlayerAction.PostAnte -> ActionType.POST_ANTE
                                is Table.Round.Action.PlayerAction.PostSmallBlind -> ActionType.POST_SB
                                is Table.Round.Action.PlayerAction.PostBigBlind -> ActionType.POST_BB
                                is Table.Round.Action.PlayerAction.PostStraddle -> ActionType.STRADDLE
                                is Table.Round.Action.PlayerAction.PostDeadBlind -> ActionType.POST_DEAD
                                is Table.Round.Action.PlayerAction.PostExtraBlind -> ActionType.POST_EXTRA_BLIND
                                is Table.Round.Action.PlayerAction.Fold -> ActionType.FOLD
                                is Table.Round.Action.PlayerAction.Check -> ActionType.CHECK
                                is Table.Round.Action.PlayerAction.Bet -> ActionType.BET
                                is Table.Round.Action.PlayerAction.Raise -> ActionType.RAISE
                                is Table.Round.Action.PlayerAction.Call -> ActionType.CALL
                                is Table.Round.Action.PlayerAction.AddChips -> ActionType.ADDED_CHIPS
                                is Table.Round.Action.PlayerAction.SitDown -> ActionType.SITS_DOWN
                                is Table.Round.Action.PlayerAction.StandUp -> ActionType.STANDS_UP
                                else -> throw IllegalStateException("This should have been filtered out")
                            },
                            amount = when (action) {
                                is Table.Round.Action.PlayerAction.PostAnte -> action.amount
                                is Table.Round.Action.PlayerAction.PostSmallBlind -> action.amount
                                is Table.Round.Action.PlayerAction.PostBigBlind -> action.amount
                                is Table.Round.Action.PlayerAction.PostStraddle -> action.amount
                                is Table.Round.Action.PlayerAction.PostDeadBlind -> action.amount
                                is Table.Round.Action.PlayerAction.PostExtraBlind -> action.amount
                                is Table.Round.Action.PlayerAction.Bet -> action.amount
                                is Table.Round.Action.PlayerAction.Raise -> action.amount
                                is Table.Round.Action.PlayerAction.Call -> action.amount
                                is Table.Round.Action.PlayerAction.AddChips -> action.amount
                                else -> null
                            },
                            isAllIn = when (action) {
                                is Table.Round.Action.PlayerAction.PostAnte -> action.isAllIn
                                is Table.Round.Action.PlayerAction.PostSmallBlind -> action.isAllIn
                                is Table.Round.Action.PlayerAction.PostBigBlind -> action.isAllIn
                                is Table.Round.Action.PlayerAction.PostStraddle -> action.isAllIn
                                is Table.Round.Action.PlayerAction.PostDeadBlind -> action.isAllIn
                                is Table.Round.Action.PlayerAction.PostExtraBlind -> action.isAllIn
                                is Table.Round.Action.PlayerAction.Bet -> action.isAllIn
                                is Table.Round.Action.PlayerAction.Raise -> action.isAllIn
                                is Table.Round.Action.PlayerAction.Call -> action.isAllIn
                                else -> null
                            },
                            cards = when (action) {
                                is Table.Round.Action.PlayerAction.DealCards -> action.cards.map {
                                    "${it.rank}${
                                        it.suit.name.first().lowercase()
                                    }"
                                }

                                is Table.Round.Action.PlayerAction.MuckCards -> action.cards.map {
                                    "${it.rank}${
                                        it.suit.name.first().lowercase()
                                    }"
                                }

                                is Table.Round.Action.PlayerAction.ShowCards -> action.cards.map {
                                    "${it.rank}${
                                        it.suit.name.first().lowercase()
                                    }"
                                }

                                else -> null
                            },
                        )
                    },
            )
        },
        pots = pots.map { pot ->
            domain.model.Pot(
                number = pot.number,
                amount = pot.amount,
                rake = null,
                jackpot = if (pot.jackpot > 0) pot.jackpot else null,
                playerWins = pot.playerWins.map { win ->
                    PlayerWin(
                        playerId = win.playerId,
                        winAmount = win.winAmount,
                        contributedRake = null,
                        cashout = null,
                        cashoutFee = null,
                        bonus = null,
                    )
                },
            )
        },
        tournamentBounties = emptyList(),
    )
)