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
    val seed: Long,
) {

    val currentRound: Round
        get() = rounds.last()

    val playerActions: List<Round.Action.PlayerAction>
        get() = rounds.flatMap { it.actions }.filterIsInstance<Round.Action.PlayerAction>()

    val currentRaise: Double
        get() = currentRound
            .actions
            .filterIsInstance<Round.Action.PlayerAction.Raise>()
            .lastOrNull()
            ?.amount ?: 0.0

    val currentRaiseByPlayer: Map<Int, Double>
        get() = currentRound
            .actions
            .filterIsInstance<Round.Action.PlayerAction.Raise>()
            .groupBy { it.playerId }
            .mapValues { it.value.maxOf { it.amount } }

    // TODO: Actually calculate current stack
    val currentStackByPlayer: Map<Int, Double>
        get() = players.associate { it.id to it.startingStack }

    val smallBlindPlayer: Player
        get() = players[dealerSeat.nextSeat()]

    val bigBlindPlayer: Player
        get() = players[dealerSeat.nextSeat().nextSeat()]

    val currentNumberOfCards: Int
        get() = rounds.sumOf {
            it.actions.filterIsInstance<Round.Action.DealCommunityCards>().sumOf { it.cards.size }
        } +
                playerActions.filterIsInstance<Round.Action.PlayerAction.DealCards>().sumOf { it.cards.size }

    fun Int.nextSeat() = (this + 1) % players.size

    enum class GameType {
        HoldEm,
    }

    data class BetLimit(
        val betType: BetType,
        val betCap: Double,
    ) {
        enum class BetType {
            NoLimit,
        }
    }

    data class Player(
        val id: Int,
        val name: String,
        val seat: Int,
        val startingStack: Double,
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

                data class AddChips(
                    override val playerId: Int,
                    val amount: Double,
                ) : PlayerAction

                data class SitDown(
                    override val playerId: Int,
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
}