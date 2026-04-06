package domain.table

import domain.model.Tournament

class TableService(
    val activeRepository: ActiveTableStateRepository,
    val historicRepository: HandHistoryRepository,
) {
    fun create(tournamentDetails: Tournament.Details) {

    }
}
