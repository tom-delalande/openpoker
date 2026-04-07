package domain.tournament

import domain.model.Table
import java.util.UUID

interface CashGameRepository {
    fun get(id: UUID): CashGame?
    fun get(): List<CashGame>

    fun save(id: UUID, game: CashGame)


    data class CashGame(
        val id: UUID,
        val tableId: UUID,
        val players: List<Player> = emptyList(),
    )

    data class Player(
        val id: Int,
        val name: String,
        val stack: Double,
    )
}