package data.inmemory

import app.logger
import domain.model.Table
import domain.table.ActiveTable
import domain.table.ActiveTableStateRepository
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

    var x = false

    override fun set(
        id: UUID,
        table: Table,
        sockets: Map<Int, Int>,
    ) {
        if (sockets.isNotEmpty()) x = true

        if (x && sockets.isEmpty()) {
            throw IllegalStateException()
        }
        tables[id] = ActiveTable(id, table, sockets)
    }
}