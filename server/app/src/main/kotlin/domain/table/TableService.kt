package domain.table

import domain.model.Table
import java.time.Instant
import java.util.UUID

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
}
