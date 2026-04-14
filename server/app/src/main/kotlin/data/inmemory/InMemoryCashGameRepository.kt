package data.inmemory

import domain.tournament.CashGameRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryCashGameRepository : CashGameRepository {
    val cashGames: MutableMap<UUID, CashGameRepository.CashGame> = ConcurrentHashMap()
    val cashGamePlayers: MutableList<CashGameRepository.Player> = mutableListOf()

    override fun get(id: UUID): CashGameRepository.CashGame? {
        return cashGames[id]
    }

    override fun get(): List<CashGameRepository.CashGame> {
        return cashGames.values.toList()
    }

    override fun save(id: UUID, game: CashGameRepository.CashGame) {
        cashGames[id] = game
    }

    override fun delete(id: UUID) {
        cashGames.remove(id)
    }

    override fun setPlayer(playerId: Int, player: CashGameRepository.Player) {
        cashGamePlayers.add(player)
    }

    override fun getPlayer(playerId: Int): CashGameRepository.Player {
        return cashGamePlayers.first { it.id == playerId }
    }
}