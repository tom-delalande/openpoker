package data.postgres

import java.sql.Types
import java.util.UUID
import kotlinx.serialization.json.Json
import org.springframework.jdbc.core.simple.JdbcClient
import server.AuthRepository

class PostgresAuthRepository(
    val jdbcClient: JdbcClient,
) : AuthRepository {
    override fun saveToken(token: UUID, playerInfo: AuthRepository.PlayerInfo) {
        jdbcClient.sql(
            """
            INSERT INTO auth_tokens (token, payload)
            VALUES (:token, :payload)
        """.trimIndent()
        )
            .param("token", token)
            .param("payload", Json.encodeToString(playerInfo), Types.OTHER)
            .update()
    }

    override fun getPlayer(token: UUID): AuthRepository.PlayerInfo? {
        return jdbcClient.sql(
            """
            SELECT payload FROM auth_tokens
            WHERE token = :token
        """.trimIndent()
        )
            .param("token", token)
            .query { result, _ ->
                Json.decodeFromString<AuthRepository.PlayerInfo>(result.getString("payload"))
            }.single()
    }
}