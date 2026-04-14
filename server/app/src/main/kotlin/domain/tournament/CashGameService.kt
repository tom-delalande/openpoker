package domain.tournament

import domain.table.ActiveTableStateRepository
import domain.table.TableService
import java.util.UUID

class CashGameService(
    private val repository: CashGameRepository,
    private val tableService: TableService,
) {
    val defaultStack = 100.0

    fun createOrJoin(playerId: Int, name: String, stack: Double): UUID {
        val games = repository.get()
        val game = games.find { it.players.size in 1..<9 } ?: CashGameRepository.CashGame()
        if (game.players.none { it.id == playerId }) {
            val updatedGame = game.copy(players = game.players + CashGameRepository.Player(playerId, name, stack))
            repository.save(game.id, updatedGame)
            tableService.createOrJoin(game.tableId, CashGameRepository.Player(playerId, name, stack))
        }

        return game.tableId
    }

    suspend fun processTables() {
        val games = repository.get()
        games.map { game ->
            val playerUpdates = tableService.process(game.tableId)
            if (playerUpdates == null) {
                repository.delete(game.id)
            } else {
                playerUpdates.forEach {
                    if (it.value.leaving) {
                        removePlayer(game.id, it.key)
                    }
                    updatePlayerStack(it.key, it.value.stack)
                }
            }
        }
    }

    fun createPlayer(playerId: Int, name: String) {
        repository.setPlayer(playerId, CashGameRepository.Player(playerId, name, defaultStack))
    }

    fun getPlayer(playerId: Int): CashGameRepository.Player {
        return repository.getPlayer(playerId)
    }

    fun updatePlayerStack(playerId: Int, stack: Double) {
        val player = repository.getPlayer(playerId)
        repository.setPlayer(playerId, player.copy(stack = stack))
    }

    fun removePlayer(gameId: UUID, playerId: Int) {
        val games = repository.get()
        val game = games.first { it.id == gameId }
        val updated = game.copy(
            players = game.players.filterNot { it.id == playerId }
        )
        repository.save(gameId, updated)
    }

    data class PlayerDataResponse(
        val playerId: Int,
        val stack: Double,
        val leaving: Boolean,
    )
}
