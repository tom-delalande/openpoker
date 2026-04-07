package domain.table

import domain.model.Table
import java.util.UUID

interface ActiveTableStateRepository {
    fun getActiveTables(): List<ActiveTable>
    fun get(id: UUID): ActiveTable?
    fun set(id: UUID, table: Table)
}

data class ActiveTable(val id: UUID, val table: Table)