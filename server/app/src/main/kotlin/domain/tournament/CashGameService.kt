package domain.tournament

import app.logger
import domain.model.Table
import domain.table.HandHistoryRepository
import domain.table.TableService
import java.util.UUID
import kotlin.collections.plus
import kotlin.math.max

class CashGameService(
    private val repository: CashGameRepository,
    private val handHistoryRepository: HandHistoryRepository,
    private val tableService: TableService,
) {
    val defaultStack = 100.0
    val minStack = 20.0

    suspend fun createOrJoin(playerId: Int, name: String, stack: Double): UUID {
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
        games.forEach { game ->
            val updatedTable = tableService.process(game.tableId)
            val playerUpdates = getPlayerUpdatesFromTable(updatedTable)
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
            if (game.players.isEmpty()) {
                repository.delete(game.id)
            }
            if (updatedTable != null && updatedTable.isFinished && updatedTable.handVersion > game.savedHandVersion) {
                handHistoryRepository.saveHand(game.tableId, updatedTable)
                repository.save(game.id, game.copy(savedHandVersion = updatedTable.handVersion))
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
        repository.setPlayer(playerId, player.copy(stack = max(stack, minStack)))
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

private fun getPlayerUpdatesFromTable(table: Table?): Map<Int, CashGameService.PlayerDataResponse>? {
    return table?.rounds?.flatMap { it.actions }?.fold(mapOf()) { players, event ->
        when (event) {
            is Table.Round.Action.PlayerAction.StandUp -> players + (event.playerId to CashGameService.PlayerDataResponse(
                event.playerId,
                event.stack,
                true,
            ))

            is Table.Round.Action.PlayerAction.SitDown -> players + (event.playerId to CashGameService.PlayerDataResponse(
                event.playerId,
                event.stack,
                false,
            ))

            is Table.Round.Action.HandEnded -> {
                val editable = players.toMutableMap()
                event.playerStacks.forEach {
                    editable[it.playerId] = editable[it.playerId]!!.copy(stack = it.stack)
                }
                editable
            }

            else -> players
        }
    }
}
