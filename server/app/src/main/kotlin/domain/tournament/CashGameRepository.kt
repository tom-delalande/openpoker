@file:UseSerializers(UUIDSerializer::class)

package domain.tournament

import common.UUIDSerializer
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

interface CashGameRepository {
    fun get(id: UUID): CashGame?
    fun get(): List<CashGame>
    fun save(id: UUID, game: CashGame)

    fun createPlayer(playerId: Int, player: Player)
    fun getPlayer(playerId: Int): Player

    @Serializable
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

    @Serializable
    data class Player(
        val id: Int,
        val name: String,
        val stack: Double,
    )
}