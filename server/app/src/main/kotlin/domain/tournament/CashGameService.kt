package domain.tournament

import domain.model.Table
import domain.table.TableService
import java.util.UUID

class CashGameService(
    val repository: CashGameRepository,
    val tableService: TableService,
) {
    fun create(game: CashGameRepository.CashGame) {
        repository.save(game.id, game)
    }

    fun join(gameId: UUID, playerId: Int, name: String, stack: Double) {
        val game = repository.get(gameId)!!
        game.copy(players = game.players + CashGameRepository.Player(playerId, name, stack))
        repository.save(game.id, game)
        tableService.receivePlayerAction(game.tableId, playerId, Table.Round.Action.PlayerAction.SitDown(playerId))
    }

    fun leave(gameId: UUID, playerId: Int) {
        val game = repository.get(gameId)!!
        game.copy(players = game.players.filterNot { it.id == playerId })
        repository.save(game.id, game)
        tableService.receivePlayerAction(game.tableId, playerId, Table.Round.Action.PlayerAction.StandUp(playerId))
    }

    fun get(): List<CashGameRepository.CashGame> {
        return emptyList()
    }

    fun get(id: UUID): CashGameRepository.CashGame? {
        return null
    }

}
