package domain.table

import domain.model.OpenHandHistory
import domain.model.Table
import java.util.UUID

interface HandHistoryRepository {
    fun saveHand(id: UUID, hand: Table)
}
