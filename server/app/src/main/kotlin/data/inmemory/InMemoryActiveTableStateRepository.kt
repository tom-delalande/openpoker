package data.inmemory

import domain.model.Table
import domain.table.ActiveTable
import domain.table.ActiveTableStateRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryActiveTableStateRepository : ActiveTableStateRepository {
    private val tables: MutableMap<UUID, ActiveTable> = ConcurrentHashMap()

    val mutex = Mutex()

    override suspend fun get(
        id: UUID,
        work: suspend (ActiveTable) -> ActiveTable,
    ): Table? {
        mutex.withLock {
            val table = tables[id] ?: return null
            val updated = work(table)
            tables[id] = updated
            return updated.table
        }
    }

    override suspend fun create(id: UUID, table: ActiveTable) {
        tables[id] = table
    }

    override fun getSession(sessionId: UUID): UUID? {
        return tables.values.find { it.playerSockets.any { it.sessionId == sessionId } && !it.finished }?.id
    }
}