package domain.tournament

import domain.model.Tournament
import java.util.UUID

class TournamentService(
    val repository: TournamentRepository,
) {
    fun create(tournament: Tournament.Details) {
    }

    fun delete(id: UUID) {
    }

    fun join(id: UUID) {
    }

    fun leave(id: UUID) {
    }

    fun start(id: UUID) {
    }

    fun stop(id: UUID) {
    }

    fun get(): List<Tournament> {
        return emptyList()
    }

    fun get(id: UUID): Tournament? {
        return null
    }
}