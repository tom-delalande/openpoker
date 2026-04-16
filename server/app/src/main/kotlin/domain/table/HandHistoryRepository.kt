package domain.table

import domain.model.Table
import java.util.UUID

interface HandHistoryRepository {
    fun saveHand(tableId: UUID, hand: Table)
}
