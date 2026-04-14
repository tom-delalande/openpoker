package data.redis

import app.logger
import domain.model.Table
import domain.table.ActiveTable
import domain.table.ActiveTableStateRepository
import domain.table.Socket
import kotlinx.serialization.json.Json
import java.util.UUID
import redis.clients.jedis.RedisClient

class RedisActiveTableRepository(
    private val redisClient: RedisClient,
) : ActiveTableStateRepository {

    companion object {
        private const val TABLE_KEY_PREFIX = "active_table:"
        private const val SESSION_KEY_PREFIX = "session:"
    }

    private val json = Json

    private fun tableKey(id: UUID) = "$TABLE_KEY_PREFIX$id"
    private fun sessionKey(sessionId: UUID) = "$SESSION_KEY_PREFIX$sessionId"

    override fun getActiveTables(): List<ActiveTable> {
        val keys = redisClient.keys("$TABLE_KEY_PREFIX*")
        return keys.mapNotNull { key ->
            try {
                json.decodeFromString<ActiveTable>(redisClient.get(key)!!)
            } catch (e: Exception) {
                logger.error("Failed to deserialize ActiveTable from key $key", e)
                null
            }
        }
    }

    override fun get(id: UUID): ActiveTable? {
        val data = redisClient.get(tableKey(id)) ?: return null
        return try {
            json.decodeFromString<ActiveTable>(data)
        } catch (e: Exception) {
            logger.error("Failed to deserialize ActiveTable for id $id", e)
            null
        }
    }

    override fun getSession(sessionId: UUID): ActiveTable? {
        val tableIdStr = redisClient.get(sessionKey(sessionId)) ?: return null
        val tableId = try {
            UUID.fromString(tableIdStr)
        } catch (e: Exception) {
            logger.error("Invalid tableId in session mapping: $tableIdStr", e)
            redisClient.del(sessionKey(sessionId))
            return null
        }
        return get(tableId)
    }

    override fun set(
        id: UUID,
        table: Table,
        finished: Boolean,
        playerSockets: List<Socket>,
    ) {
        val activeTable = ActiveTable(id, table, playerSockets, finished)
        try {
            redisClient.set(tableKey(id), json.encodeToString(activeTable))
            if (!finished) {
                playerSockets.forEach { socket ->
                    redisClient.set(sessionKey(socket.sessionId), id.toString())
                }
            } else {
                playerSockets.forEach { socket ->
                    redisClient.del(sessionKey(socket.sessionId))
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to persist ActiveTable for id $id", e)
        }
    }
}