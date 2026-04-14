package data.inmemory

import domain.model.Table
import domain.table.HandHistoryRepository
import java.util.UUID

class InMemoryHandHistoryRepository : HandHistoryRepository {
    override fun saveHand(tableId: UUID, hand: Table) {
        // TODO: [low] Save hand history
    }
}