@file:UseSerializers(UUIDSerializer::class)

package domain.table

import common.UUIDSerializer
import domain.model.Table
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

interface ActiveTableStateRepository {
    fun getActiveTables(): List<ActiveTable>
    suspend fun performedLockedFunctionOnTables(work: suspend (List<ActiveTable>) -> Unit)
    fun get(id: UUID): ActiveTable?
    suspend fun get(id: UUID, work: suspend (ActiveTable) -> Unit): Table?
    fun getSession(sessionId: UUID): ActiveTable?
    fun set(id: UUID, table: Table, finished: Boolean, playerSockets: List<Socket>, withLock: Boolean = true)
}

@Serializable
data class ActiveTable(val id: UUID, val table: Table, val playerSockets: List<Socket>, val finished: Boolean)

@Serializable
data class Socket(
    val playerId: Int,
    val sessionId: UUID,
    val currentHandId: UUID,
    val version: Int,
)