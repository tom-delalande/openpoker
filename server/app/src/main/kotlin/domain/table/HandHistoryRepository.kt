package domain.table

import domain.table.model.OpenHandHistory
import java.util.UUID

interface HandHistoryRepository {
    fun saveHand(id: UUID, hand: OpenHandHistory)
    fun getHandById(id: UUID): OpenHandHistory
    fun listHandIdsByTournamentId(id: UUID): List<UUID>
}
