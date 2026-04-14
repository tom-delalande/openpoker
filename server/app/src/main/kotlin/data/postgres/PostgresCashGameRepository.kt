package data.postgres

import domain.tournament.CashGameRepository
import java.sql.Types
import java.util.UUID
import kotlinx.serialization.json.Json
import org.springframework.jdbc.core.simple.JdbcClient

class PostgresCashGameRepository(
    val jdbcClient: JdbcClient,
) : CashGameRepository {
    override fun get(id: UUID): CashGameRepository.CashGame? {
        return jdbcClient.sql(
            """
            SELECT payload
            FROM cash_games
            WHERE id = :id
        """.trimIndent()
        )
            .param("id", id)
            .query { result, _ ->
                Json.decodeFromString<CashGameRepository.CashGame>(result.getString("payload"))
            }.single()
    }

    override fun get(): List<CashGameRepository.CashGame> {
        return jdbcClient.sql(
            """
            SELECT payload
            FROM cash_games
        """.trimIndent()
        )
            .query { result, _ ->
                Json.decodeFromString<CashGameRepository.CashGame>(result.getString("payload"))
            }.list()
    }

    override fun save(id: UUID, game: CashGameRepository.CashGame) {
        jdbcClient.sql(
            """
            INSERT INTO cash_games (id, payload)
            VALUES (:id, :payload)
        """.trimIndent()
        )
            .param("id", id)
            .param("payload", Json.encodeToString(game), Types.OTHER)
            .update()
    }

    override fun createPlayer(playerId: Int, player: CashGameRepository.Player) {
        jdbcClient.sql(
            """
            INSERT INTO cash_game_players (id, payload)
            VALUES (:id, :payload)
        """.trimIndent()
        )
            .param("id", playerId)
            .param("payload", Json.encodeToString(player), Types.OTHER)
            .update()
    }

    override fun getPlayer(playerId: Int): CashGameRepository.Player {
        return jdbcClient.sql(
            """
            SELECT payload
            FROM cash_game_players
            WHERE id = :id
        """.trimIndent()
        )
            .param("id", playerId)
            .query { result, _ ->
                Json.decodeFromString<CashGameRepository.Player>(result.getString("payload"))
            }.single()
    }

}