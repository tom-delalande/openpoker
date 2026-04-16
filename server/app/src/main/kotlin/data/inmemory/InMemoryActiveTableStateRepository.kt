package data.inmemory

import domain.model.Table
import domain.table.ActiveTable
import domain.table.ActiveTableStateRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryActiveTableStateRepository : ActiveTableStateRepository {
    private val tables: MutableMap<UUID, ActiveTable> = ConcurrentHashMap()

    override suspend fun get(
        id: UUID,
        work: suspend (ActiveTable) -> ActiveTable,
    ): Table? {
        val table = tables[id] ?: return null
        return work(table).table
    }

    override suspend fun create(id: UUID, table: ActiveTable) {
        tables[id] = table
    }

    override fun getSession(sessionId: UUID): UUID? {
        return tables.values.find { it.playerSockets.any { it.sessionId == sessionId } && !it.finished }?.id
    }
}