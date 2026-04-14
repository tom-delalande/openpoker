package data.postgres

import domain.model.Table
import domain.table.HandHistoryRepository
import java.sql.Types
import java.util.UUID
import kotlinx.serialization.json.Json
import org.springframework.jdbc.core.simple.JdbcClient

class PostgresHandHistoryRepository(
    val jdbcClient: JdbcClient,
) : HandHistoryRepository {

    override fun saveHand(tableId: UUID, hand: Table) {
        val openHandHistory = hand.toOpenHandHistory()

        jdbcClient.sql(
            """
            INSERT INTO hand_history (table_id, hand_id, payload)
            VALUES (:tableId, :handId, :payload)
        """.trimIndent()
        )
            .param("tableId", tableId)
            .param("handId", hand.handId)
            .param("payload", Json.encodeToString(openHandHistory), Types.OTHER)
            .update()
    }
}