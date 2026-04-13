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

    override fun get(id: UUID): ActiveTable? {
        return tables[id]
    }

    override fun getSession(sessionId: UUID): ActiveTable? {
        return tables.values.find { it.playerSockets.any { it.sessionId == sessionId } && !it.finished }
    }

    override fun set(
        id: UUID,
        table: Table,
        finished: Boolean,
        playerSockets: List<Socket>,
    ) {
        tables[id] = ActiveTable(id, table, playerSockets, finished)
    }
}