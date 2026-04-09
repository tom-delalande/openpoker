package domain.tournament

import domain.model.Table
import java.util.UUID

interface CashGameRepository {
    fun get(id: UUID): CashGame?
    fun get(): List<CashGame>

    fun save(id: UUID, game: CashGame)


    data class CashGame(
        val id: UUID = UUID.randomUUID(),
        val tableId: UUID = UUID.randomUUID(),
        val status: GameStatus = GameStatus.Registering,
        val players: List<Player> = emptyList(),
    )

    enum class GameStatus {
        Registering,
        Playing,
        Finished,
    }

    data class Player(
        val id: Int,
        val name: String,
        val stack: Double,
    )
}