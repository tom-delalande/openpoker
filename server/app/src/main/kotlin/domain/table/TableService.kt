package domain.table

import java.time.Instant

class TableService(
    val activeRepository: ActiveTableStateRepository,
    val historicRepository: HandHistoryRepository,
) {
    fun process(now: Instant) {
        val tables = activeRepository.getActiveTables()
        tables.forEach { (id, table) ->
            val updated = table.processTable(now)
            activeRepository.set(id, updated)
            if (table.isFinished) {
                historicRepository.saveHand(id, updated)
            }
        }
    }
}
