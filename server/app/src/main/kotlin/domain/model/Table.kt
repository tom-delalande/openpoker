@file:UseSerializers(
    UUIDSerializer::class,
    InstantSerializer::class,
)

package domain.model

import common.InstantSerializer
import common.UUIDSerializer
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class Table(
    val handId: UUID,
    val gameType: GameType,
    val betLimit: BetLimit,
    val tableSize: Int,
    val dealerSeat: Int,
    val smallBlindAmount: Double,
    val bigBlindAmount: Double,
    val anteAmount: Double,
    val rounds: List<Round>,
    val pots: List<Pot>,
    val isFinished: Boolean = false,
    val isStarted: Boolean = false,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val seed: Long,
    val players: List<Player> = emptyList(),
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

    val dealerPlayer: LivePlayerInfo
        get() = livePlayers.sortedBy { it.seat }.shift(dealerSeat).first()

    val smallBlindPlayer: LivePlayerInfo
        get() = dealerPlayer.nextPlayer()

    val bigBlindPlayer: LivePlayerInfo
        get() = smallBlindPlayer.nextPlayer()

    val currentNumberOfCards: Int
        get() = rounds.sumOf {
            it.actions.filterIsInstance<Round.Action.DealCommunityCards>().sumOf { it.cards.size }
        } + rounds.flatMap { it.actions }.filterIsInstance<Round.Action.PlayerAction.DealCards>()
            .sumOf { it.cards.size }

    fun Int.nextSeat() = (this + 1) % livePlayers.size

    enum class GameType {
        HoldEm,
    }

    @Serializable
    data class BetLimit(
        val betType: BetType,
        val betCap: Double?,
    ) {
        enum class BetType {
            NoLimit,
        }
    }

    @Serializable
    data class Player(
        val id: Int,
        val name: String,
        val seat: Int,
        val stack: Double,
        val isSittingOut: Boolean,
    )

    @Serializable
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

        @Serializable
        sealed interface Action {
            @Serializable
            sealed interface PlayerAction : Action {
                // TODO: [medium] this should really use seatId
                val playerId: Int

                @Serializable
                data class RequestAction(
                    override val playerId: Int,
                    val actionOptions: List<ActionOption>,
                    val expiry: Instant,
                ) : PlayerAction {
                    @Serializable
                    sealed interface ActionOption {
                        @Serializable
                        object MuckCards : ActionOption

                        @Serializable
                        object ShowCards : ActionOption

                        @Serializable
                        data class PostAnte(val amount: Double) : ActionOption

                        @Serializable
                        data class PostSmallBlind(val amount: Double) : ActionOption

                        @Serializable
                        data class PostBigBlind(val amount: Double) : ActionOption

                        @Serializable
                        data class PostStraddle(val amount: Double) : ActionOption

                        @Serializable
                        data class PostDeadBlind(val amount: Double) : ActionOption

                        @Serializable
                        data class PostExtraBlind(val amount: Double) : ActionOption

                        @Serializable
                        object Fold : ActionOption

                        @Serializable
                        object Check : ActionOption

                        @Serializable
                        data class Bet(val minAmount: Double, val maxAmount: Double?) : ActionOption

                        @Serializable
                        data class Call(val amount: Double) : ActionOption

                        @Serializable
                        data class Raise(val minAmount: Double, val maxAmount: Double?) : ActionOption
                    }
                }

                @Serializable
                data class DealCards(
                    override val playerId: Int,
                    val cards: List<Card>,
                ) : PlayerAction

                @Serializable
                data class MuckCards(
                    override val playerId: Int,
                    val cards: List<Card>,
                ) : PlayerAction

                @Serializable
                data class ShowCards(
                    override val playerId: Int,
                    val cards: List<Card>,
                ) : PlayerAction

                @Serializable
                data class PostAnte(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                @Serializable
                data class PostSmallBlind(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                @Serializable
                data class PostBigBlind(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                @Serializable
                data class PostStraddle(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                @Serializable
                data class PostDeadBlind(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                @Serializable
                data class PostExtraBlind(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                @Serializable
                data class Fold(
                    override val playerId: Int,
                ) : PlayerAction

                @Serializable
                data class Check(
                    override val playerId: Int,
                ) : PlayerAction

                @Serializable
                data class Bet(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                @Serializable
                data class Raise(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                @Serializable
                data class Call(
                    override val playerId: Int,
                    val amount: Double,
                    val isAllIn: Boolean,
                ) : PlayerAction

                @Serializable
                data class AddChips(
                    override val playerId: Int,
                    val amount: Double,
                ) : PlayerAction

                @Serializable
                data class SitDown(
                    override val playerId: Int,
                    val playerName: String,
                    val seat: Int,
                    val stack: Double,
                ) : PlayerAction

                @Serializable
                data class StandUp(
                    override val playerId: Int,
                ) : PlayerAction
            }


            @Serializable
            data class DealCommunityCards(
                val cards: List<Card>,
            ) : Action

            @Serializable
            data object HandStarted : Action

            @Serializable
            data object HandEnded : Action

            @Serializable
            data class RoundStarted(
                val id: Int,
                val street: Street,
            ) : Action
        }
    }

    @Serializable
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

    @Serializable
    data class Pot(
        val number: Int,
        val amount: Double,
        val jackpot: Double,
        val playerWins: List<PlayerWin>,
    ) {
        @Serializable
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

    val lastActivePlayerToAct: LivePlayerInfo
        get() = livePlayers
            .find { it.playerId == playerRoundActions.lastOrNull { it !is Round.Action.PlayerAction.DealCards }?.playerId }
            ?: smallBlindPlayer

    fun LivePlayerInfo.nextPlayerToAct(): LivePlayerInfo {
        return livePlayers.sortedBy { it.seat }.shift(seat + 1).first { !it.isOut && !it.isAllIn && !it.isSittingOut }
    }

    fun LivePlayerInfo.nextPlayer(): LivePlayerInfo {
        return livePlayers.sortedBy { it.seat }.shift(seat + 1).first()
    }

    val nextPlayerToAct: LivePlayerInfo
        get() = lastActivePlayerToAct.nextPlayerToAct()

    val livePlayers: List<LivePlayerInfo>
        get() = rounds.flatMap { it.actions }.fold(listOf()) { players, action ->
            when (action) {
                Round.Action.HandStarted -> players
                Round.Action.HandEnded -> players.map { it.copy(isAllIn = false) }
                is Round.Action.RoundStarted -> players
                    .filterNot { it.isSittingOut }
                    .map { it.copy(contributionThisStreet = 0.0) }

                is Round.Action.DealCommunityCards -> players.map { it.copy(cards = it.cards + action.cards) }
                is Round.Action.PlayerAction -> {
                    if (action is Round.Action.PlayerAction.SitDown) {
                        return@fold players.filterNot { it.playerId == action.playerId } + LivePlayerInfo(
                            playerId = action.playerId,
                            seat = action.seat,
                            name = action.playerName,
                            stack = action.stack,
                            isAllIn = false,
                        )
                    }
                    val player = players.firstOrNull { it.playerId == action.playerId } ?: return@fold players
                    val updatedPlayer = when (action) {
                        is Round.Action.PlayerAction.Bet -> player.copy(
                            contributionThisStreet = player.contributionThisStreet + action.amount,
                            stack = player.stack - action.amount,
                            isAllIn = action.isAllIn,
                            lastAction = action,
                        )

                        is Round.Action.PlayerAction.Call -> player.copy(
                            contributionThisStreet = player.contributionThisStreet + action.amount,
                            stack = player.stack - action.amount,
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
                            stack = player.stack - action.amount,
                            isAllIn = action.isAllIn,
                            lastAction = action,
                        )

                        is Round.Action.PlayerAction.PostBigBlind -> player.copy(
                            contributionThisStreet = player.contributionThisStreet + action.amount,
                            stack = player.stack - action.amount,
                            isAllIn = action.isAllIn,
                            lastAction = action,
                        )

                        is Round.Action.PlayerAction.PostDeadBlind -> player.copy(
                            contributionThisStreet = player.contributionThisStreet + action.amount,
                            stack = player.stack - action.amount,
                            isAllIn = action.isAllIn,
                            lastAction = action,
                        )

                        is Round.Action.PlayerAction.PostExtraBlind -> player.copy(
                            contributionThisStreet = player.contributionThisStreet + action.amount,
                            stack = player.stack - action.amount,
                            isAllIn = action.isAllIn,
                            lastAction = action,
                        )

                        is Round.Action.PlayerAction.PostSmallBlind -> player.copy(
                            contributionThisStreet = player.contributionThisStreet + action.amount,
                            stack = player.stack - action.amount,
                            isAllIn = action.isAllIn,
                            lastAction = action,
                        )

                        is Round.Action.PlayerAction.PostStraddle -> player.copy(
                            contributionThisStreet = player.contributionThisStreet + action.amount,
                            stack = player.stack - action.amount,
                            isAllIn = action.isAllIn,
                            lastAction = action,
                        )

                        is Round.Action.PlayerAction.Raise -> player.copy(
                            contributionThisStreet = player.contributionThisStreet + action.amount,
                            stack = player.stack - action.amount,
                            isAllIn = action.isAllIn,
                            lastAction = action,
                        )

                        is Round.Action.PlayerAction.SitDown -> return@fold players + LivePlayerInfo(
                            playerId = action.playerId,
                            seat = action.seat,
                            name = action.playerName,
                            stack = action.stack,
                        )

                        is Round.Action.PlayerAction.StandUp -> return@fold players.map {
                            if (it.playerId == action.playerId) it.copy(
                                isSittingOut = true
                            ) else it
                        }

                        is Round.Action.PlayerAction.MuckCards,
                        is Round.Action.PlayerAction.RequestAction,
                        is Round.Action.PlayerAction.ShowCards,
                            -> player

                        is Round.Action.PlayerAction.AddChips -> TODO()
                    }
                    players.map { if (it.playerId == player.playerId) updatedPlayer else it }
                }

            }
        }

    data class LivePlayerInfo(
        val playerId: Int,
        val seat: Int,
        val name: String,
        val stack: Double,
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