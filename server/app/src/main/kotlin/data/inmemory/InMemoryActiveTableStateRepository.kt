package data.inmemory

import app.logger
import domain.model.Table
import domain.table.ActiveTable
import domain.table.ActiveTableStateRepository
import domain.table.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryActiveTableStateRepository : ActiveTableStateRepository {
    private val tables: MutableMap<UUID, ActiveTable> = ConcurrentHashMap()

    override fun getActiveTables(): List<ActiveTable> {
        return tables.values.toList()
    }

    override suspend fun performedLockedFunctionOnTables(work: suspend (List<ActiveTable>) -> Unit) {
        work(getActiveTables())
    }

    override fun get(id: UUID): ActiveTable? {
        return tables[id]
    }

    override suspend fun get(
        id: UUID,
        work: suspend (ActiveTable) -> ActiveTable,
    ): Table? {
        return work(get(id)!!).table
    }

    override fun getSession(sessionId: UUID): UUID? {
        return tables.values.find { it.playerSockets.any { it.sessionId == sessionId } && !it.finished }?.id
    }

    override fun set(id: UUID, table: ActiveTable, withLock: Boolean) {
        tables[id] = table
    }
}