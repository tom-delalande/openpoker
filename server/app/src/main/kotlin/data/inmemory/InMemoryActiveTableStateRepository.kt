package data.inmemory

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

    override fun set(
        id: UUID,
        table: Table,
        sockets: Map<Int, Int>,
    ) {
        tables[id] = ActiveTable(id, table, sockets)
    }

}