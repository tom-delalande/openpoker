package domain.tournament

import domain.model.Table
import domain.table.TableService
import domain.table.createTable
import java.util.UUID

class CashGameService(
    private val repository: CashGameRepository,
    private val tableService: TableService,
) {

    fun createOrJoin(playerId: Int, name: String): UUID {
        val defaultStack = 500.0

        val games = repository.get()
        val game = games.find {
            it.status == CashGameRepository.GameStatus.Registering
        } ?: CashGameRepository.CashGame()

        val updatedGame = game.copy(
            players = game.players + CashGameRepository.Player(playerId, name, defaultStack)
        )

        tableService.saveTable(updatedGame.tableId, createTable(updatedGame.players), mapOf())

        repository.save(game.id, updatedGame)
        return updatedGame.tableId
    }
}
