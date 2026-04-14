package domain.tournament

import domain.table.TableService
import java.util.UUID

class CashGameService(
    private val repository: CashGameRepository,
    private val tableService: TableService,
) {
    val defaultStack = 100.0

    fun createOrJoin(playerId: Int, name: String, stack: Double): UUID {
        val games = repository.get()
        val game = games.find {
            it.players.size < 9
        } ?: CashGameRepository.CashGame()
        if (game.players.none { it.id == playerId }) {
            val updatedGame = game.copy(
                players = game.players + CashGameRepository.Player(playerId, name, stack)
            )
            tableService.createOrJoin(game.tableId, CashGameRepository.Player(playerId, name, stack))
            repository.save(game.id, updatedGame)
        } else {
            tableService.createOrJoin(game.tableId, CashGameRepository.Player(playerId, name, stack))
        }

        return game.tableId
    }

    fun createPlayer(playerId: Int, name: String) {
        repository.createPlayer(playerId, CashGameRepository.Player(playerId, name, defaultStack))
    }

    fun getPlayer(playerId: Int): CashGameRepository.Player {
        return repository.getPlayer(playerId)
    }

    fun updateGameInformation(gameId: UUID) {
        val game = repository.get(gameId)
    }

    fun removePlayer(gameId: UUID, playerId: Int) {
        val games = repository.get()
        val game = games.first { it.id == gameId }
        val updated = game.copy(
            players = game.players.filterNot { it.id == playerId }
        )
        repository.save(gameId, updated)
    }
}
