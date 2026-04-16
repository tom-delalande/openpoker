@file:UseSerializers(UUIDSerializer::class)

package domain.table

import common.UUIDSerializer
import domain.model.Table
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

interface ActiveTableStateRepository {
    suspend fun get(id: UUID, work: suspend (ActiveTable) -> ActiveTable): Table?
    suspend fun create(id: UUID, table: ActiveTable)
    fun getSession(sessionId: UUID): UUID?
}

@Serializable
data class ActiveTable(val id: UUID, val table: Table, val playerSockets: List<Socket>, val finished: Boolean)

@Serializable
data class Socket(
    val playerId: Int,
    val sessionId: UUID,
    val tableId: UUID,
    val handVersion: Int,
    val version: Int,
)