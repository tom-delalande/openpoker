package data.inmemory

import domain.tournament.CashGameRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryCashGameRepository : CashGameRepository {
    val cashGames: MutableMap<UUID, CashGameRepository.CashGame> = ConcurrentHashMap()

    override fun get(id: UUID): CashGameRepository.CashGame? {
        return cashGames[id]
    }

    override fun get(): List<CashGameRepository.CashGame> {
        return cashGames.values.toList()
    }

    override fun save(id: UUID, game: CashGameRepository.CashGame) {
        cashGames[id] = game
    }
}