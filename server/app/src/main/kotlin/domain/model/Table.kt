package domain.model

import java.time.Instant

data class Table(
    val gameType: GameType,
    val betLimit: BetLimit,
    val tableSize: Int,
    val dealerSeat: Int,
    val smallBlindAmount: Double,
    val bigBlindAmount: Double,
    val anteAmount: Double,
    val players: List<Player>,
    val rounds: List<Round>,
    val pots: List<Pot>,
    val isFinished: Boolean = false,
    val seed: Long,
) {
    val currentRound: Round?
        get() = rounds.lastOrNull()

    val playerRoundActions: List<Round.Action.PlayerAction>
        get() = currentRound?.actions?.filterIsInstance<Round.Action.PlayerAction>() ?: emptyList()

    val currentRaise: Double
        get() = livePlayers.maxOfOrNull { it.contributionThisStreet } ?: bigBlindAmount

    val previousRaise: Double
        get() = livePlayers.map { it.contributionThisStreet }.sortedDescending()
            .getOrElse(2) { 0.0 }

    val smallBlindPlayer: Player
        get() = players[dealerSeat.nextSeat()]

    val bigBlindPlayer: Player
        get() = players[dealerSeat.nextSeat().nextSeat()]

    val currentNumberOfCards: Int
        get() = rounds.sumOf {
            it.actions.filterIsInstance<Round.Action.DealCommunityCards>().sumOf { it.cards.size }
        } + rounds.flatMap { it.actions }.filterIsInstance<Round.Action.PlayerAction.DealCards>()
            .sumOf { it.cards.size }

    fun Int.nextSeat() = (this + 1) % players.size

    // TODO: [high] make this actually use active seat
    fun Int.nextActiveSeat() = (this + 1) % players.size

    enum class GameType {
        HoldEm,
    }

    data class BetLimit(
        val betType: BetType,
        val betCap: Double?,
    ) {
        enum class BetType {
            NoLimit,
        }
    }

    data class Player(
        val id: Int,
        val name: String,
        val seat: Int,
        val stack: Double,
        val isSittingOut: Boolean,
    )

    data class Round(
        val id: Int,
        val street: Street,
        val actions: List<Action>,
    ) {
        enum class Street {
            PreFlop,
            Flop,
            Turn,
            River,
            Showdown,
        }

        sealed interface Action {
            sealed interface PlayerAction : Action {
                // TODO: [medium] this should really use seatId
                val playerId: Int

                data class RequestAction(
                    override val playerId: Int,
                    val actionOptions: List<ActionOption>,
                    val expiry: Instant,
                ) : PlayerAction {
                    sealed interface ActionOption {
                        object MuckCards : ActionOption
                        object ShowCards : ActionOption
                        data class PostAnte(val amount: Double) : ActionOption
                        data class PostSmallBlind(val amount: Double) : ActionOption
                        data class PostBigBlind(val amount: Double) : ActionOption
                        data class PostStraddle(val amount: Double) : ActionOption
                        data class PostDeadBlind(val amount: Double) : ActionOption
                        data class PostExtraBlind(val amount: Double) : ActionOption
                        object Fold : ActionOption
                        object Check : ActionOption
                        data class Bet(val minAmount: Double, val maxAmount: Double?) : ActionOption
                        data class Call(val amount: Double) : ActionOption
                        data class Raise(val minAmount: Double, val maxAmount: Double?) : ActionOption
                    }
                }

                data class DealCards(
                    override val playerId: Int,
                    val cards: List<Card>,
                ) : PlayerAction

                data class MuckCards(
                    override val playerId: Int,
                    val cards: List<Card>,
                ) : PlayerAction

                data class ShowCards(
                    override val playerId: Int,
                    val cards: List<Card>,
                ) : PlayerAction

                data class PostAnte(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                data class PostSmallBlind(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                data class PostBigBlind(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                data class PostStraddle(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                data class PostDeadBlind(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                data class PostExtraBlind(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                data class Fold(
                    override val playerId: Int,
                ) : PlayerAction

                data class Check(
                    override val playerId: Int,
                ) : PlayerAction

                data class Bet(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                data class Raise(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                data class Call(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                data class AddChips(
                    override val playerId: Int,
                    val amount: Double,
                ) : PlayerAction

                data class SitDown(
                    override val playerId: Int,
                    val playerName: String,
                    val seat: Int,
                    val stack: Double,
                ) : PlayerAction

                data class StandUp(
                    override val playerId: Int,
                ) : PlayerAction
            }


            data class DealCommunityCards(
                val cards: List<Card>,
            ) : Action
        }
    }

    data class Card(
        val suit: Suit,
        val rank: Int,
    ) {
        enum class Suit {
            Hearts,
            Diamonds,
            Spades,
            Clubs,
        }
    }

    data class Pot(
        val number: Int,
        val amount: Double,
        val jackpot: Double,
        val playerWins: List<PlayerWin>,
    ) {
        data class PlayerWin(
            val playerId: Int,
            val winAmount: Double,
        )
    }


    // TODO: [high] check this is right
    val pot: Double
        get() = rounds.fold(0.0) { pot, round ->
            round.actions.fold(pot) { pot, action ->
                when (action) {
                    is Round.Action.PlayerAction.Bet -> pot + action.amount
                    is Round.Action.PlayerAction.Call -> pot + action.amount
                    is Round.Action.PlayerAction.PostAnte -> pot + action.amount
                    is Round.Action.PlayerAction.PostBigBlind -> pot + action.amount
                    is Round.Action.PlayerAction.PostDeadBlind -> pot + action.amount
                    is Round.Action.PlayerAction.PostExtraBlind -> pot + action.amount
                    is Round.Action.PlayerAction.PostSmallBlind -> pot + action.amount
                    is Round.Action.PlayerAction.PostStraddle -> pot + action.amount
                    is Round.Action.PlayerAction.Raise -> pot + action.amount
                    else -> pot
                }

            }
        }

    fun LivePlayerInfo.nextPlayerToAct(): LivePlayerInfo {
        return livePlayers.sortedBy { it.seat }.shift(seat + 1).first { !it.isOut && !it.isAllIn }
    }

    val livePlayers: List<LivePlayerInfo>
        get() = rounds.fold(players.map {
            LivePlayerInfo(
                playerId = it.id,
                seat = it.seat,
                position = (it.seat - dealerSeat) % players.size,
                currentStack = it.stack,
            )
        }) { players, round ->
            return@fold round.actions.fold(players.map { it.copy(contributionThisStreet = 0.0) }) { players, action ->
                players.map { player ->
                    if (action is Round.Action.PlayerAction && player.playerId == action.playerId || action !is Round.Action.PlayerAction) {
                        when (action) {
                            is Round.Action.DealCommunityCards -> player.copy(cards = player.cards + action.cards)
                            is Round.Action.PlayerAction.Bet -> player.copy(
                                contributionThisStreet = player.contributionThisStreet + action.amount,
                                currentStack = player.currentStack - action.amount,
                                isAllIn = action.isAllIn,
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.Call -> player.copy(
                                contributionThisStreet = player.contributionThisStreet + action.amount,
                                currentStack = player.currentStack - action.amount,
                                isAllIn = action.isAllIn,
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.Check -> player.copy(
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.DealCards -> player.copy(
                                cards = player.cards + action.cards,
                                pocketCards = player.pocketCards + action.cards
                            )

                            is Round.Action.PlayerAction.Fold -> player.copy(isOut = true, lastAction = action)
                            is Round.Action.PlayerAction.PostAnte -> player.copy(
                                contributionThisStreet = player.contributionThisStreet + action.amount,
                                currentStack = player.currentStack - action.amount,
                                isAllIn = action.isAllIn,
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.PostBigBlind -> player.copy(
                                contributionThisStreet = player.contributionThisStreet + action.amount,
                                currentStack = player.currentStack - action.amount,
                                isAllIn = action.isAllIn,
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.PostDeadBlind -> player.copy(
                                contributionThisStreet = player.contributionThisStreet + action.amount,
                                currentStack = player.currentStack - action.amount,
                                isAllIn = action.isAllIn,
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.PostExtraBlind -> player.copy(
                                contributionThisStreet = player.contributionThisStreet + action.amount,
                                currentStack = player.currentStack - action.amount,
                                isAllIn = action.isAllIn,
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.PostSmallBlind -> player.copy(
                                contributionThisStreet = player.contributionThisStreet + action.amount,
                                currentStack = player.currentStack - action.amount,
                                isAllIn = action.isAllIn,
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.PostStraddle -> player.copy(
                                contributionThisStreet = player.contributionThisStreet + action.amount,
                                currentStack = player.currentStack - action.amount,
                                isAllIn = action.isAllIn,
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.Raise -> player.copy(
                                contributionThisStreet = player.contributionThisStreet + action.amount,
                                currentStack = player.currentStack - action.amount,
                                isAllIn = action.isAllIn,
                                lastAction = action,
                            )

                            is Round.Action.PlayerAction.SitDown -> player.copy(isSittingOut = false)
                            is Round.Action.PlayerAction.StandUp -> player.copy(isSittingOut = true)
                            is Round.Action.PlayerAction.AddChips -> TODO()
                            is Round.Action.PlayerAction.MuckCards,
                            is Round.Action.PlayerAction.RequestAction,
                            is Round.Action.PlayerAction.ShowCards,
                                -> player
                        }
                    } else {
                        player
                    }
                }
            }
        }

    data class LivePlayerInfo(
        val playerId: Int,
        val seat: Int,
        val position: Int, // 0 is dealer, 1 is small blind ...
        val currentStack: Double,
        val contributionThisStreet: Double = 0.0,
        val isOut: Boolean = false,
        val isSittingOut: Boolean = false,
        val isAllIn: Boolean = false,
        val lastAction: Round.Action.PlayerAction? = null,
        val cards: List<Card> = emptyList(),
        val pocketCards: List<Card> = emptyList(),
    )
}

internal fun <T> List<T>.shift(shift: Int): List<T> {
    if (isEmpty()) return this
    val n = size
    val k = ((shift % n) + n) % n
    return drop(k) + take(k)
}