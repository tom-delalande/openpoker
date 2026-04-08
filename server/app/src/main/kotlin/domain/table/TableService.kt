package domain.table

import domain.model.Table
import java.time.Instant
import java.util.UUID
import server.TableEvent

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

    fun Table.toEvents(): List<server.HandEvent> = buildList {
        if (rounds.isNotEmpty()) {
            // add()
        }

        rounds.forEach { round ->
            if (round.id == 0) {
                // add(HandStartedEvent)
            }
            // add(RoundStartedEvent)
            round.actions.map { action ->
                when (action) {
                    is Table.Round.Action.DealCommunityCards -> add(TableEvent())
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
}
