package domain.table

import domain.model.Table
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.ExperimentalSerializationApi
import server.models.CommunityCardDealt
import server.models.CommunityCardDealtType
import server.models.HandEvent
import server.models.HandEventCommunityCardDealt
import server.models.HandEventHandStarted
import server.models.HandEventHandStartedEvent
import server.models.HandEventRoundStarted
import server.models.HandStarted
import server.models.HandStartedEvent
import server.models.HandStartedEventType
import server.models.HandStartedType
import server.models.RoundStarted
import server.models.RoundStartedType

class TableService(
    val activeRepository: ActiveTableStateRepository,
    val historicRepository: HandHistoryRepository,
) {
    fun process(now: Instant = Instant.now()) {
        val tables = activeRepository.getActiveTables()
        tables.forEach { (id, table) ->
            val updated = table.processTable(now)
            if (table.isFinished) {
                historicRepository.saveHand(id, updated)
                val nextHand = table.startNextHand()
                activeRepository.set(id, nextHand)
            } else {
                activeRepository.set(id, updated)
            }
        }
    }

    fun receivePlayerAction(
        tableId: UUID,
        playerId: Int,
        action: Table.Round.Action.PlayerAction,
        now: Instant = Instant.now(),
    ) {
        val activeTable = activeRepository.get(tableId)!!
        val table = activeTable.table
        table.processPlayerAction(playerId, action, now)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun Table.toEvents(): List<HandEvent> = buildList {
        rounds.forEach { round ->
            if (round.id == 0) {
                add(
                    HandEventHandStarted(
                        value = HandStarted(
                            type = HandStartedType.HAND_STARTED_EVENT,
                            dealerButton = dealerSeat,
                        )
                    )
                )
            }
            add(
                HandEventRoundStarted(
                    value = RoundStarted(
                        type = RoundStartedType.ROUND_STARTED_EVENT,
                        street = round.street.name
                    )
                )
            )

            round.actions.map { action ->
                when (action) {
                    is Table.Round.Action.DealCommunityCards -> add(
                        HandEventCommunityCardDealt(
                            value = CommunityCardDealt(
                                type = CommunityCardDealtType.COMMUNITY_CARD_DEALT_EVENT,
                                cards = action.cards.map { it.toStringValue() }
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.AddChips -> TODO()
                    is Table.Round.Action.PlayerAction.Bet -> TODO()
                    is Table.Round.Action.PlayerAction.Call -> TODO()
                    is Table.Round.Action.PlayerAction.Check -> TODO()
                    is Table.Round.Action.PlayerAction.DealCards -> TODO()
                    is Table.Round.Action.PlayerAction.Fold -> TODO()
                    is Table.Round.Action.PlayerAction.MuckCards -> TODO()
                    is Table.Round.Action.PlayerAction.PostAnte -> TODO()
                    is Table.Round.Action.PlayerAction.PostBigBlind -> TODO()
                    is Table.Round.Action.PlayerAction.PostDeadBlind -> TODO()
                    is Table.Round.Action.PlayerAction.PostExtraBlind -> TODO()
                    is Table.Round.Action.PlayerAction.PostSmallBlind -> TODO()
                    is Table.Round.Action.PlayerAction.PostStraddle -> TODO()
                    is Table.Round.Action.PlayerAction.Raise -> TODO()
                    is Table.Round.Action.PlayerAction.RequestAction -> TODO()
                    is Table.Round.Action.PlayerAction.ShowCards -> TODO()
                    is Table.Round.Action.PlayerAction.SitDown -> TODO()
                    is Table.Round.Action.PlayerAction.StandUp -> TODO()
                }
            }
        }
    }

    fun Table.Card.toStringValue(): String {
        val suit = when (suit) {
            Table.Card.Suit.Hearts -> "h"
            Table.Card.Suit.Diamonds -> "d"
            Table.Card.Suit.Spades -> "s"
            Table.Card.Suit.Clubs -> "c"
        }
        return "$rank$suit"
    }
}
