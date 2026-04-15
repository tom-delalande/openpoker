package data.redis

import app.logger
import domain.model.Table
import domain.table.ActiveTable
import domain.table.ActiveTableStateRepository
import java.time.Duration
import kotlinx.serialization.json.Json
import java.util.UUID
import java.time.Instant
import redis.clients.jedis.RedisClient
import redis.clients.jedis.params.SetParams

class RedisActiveTableRepository(
    private val redisClient: RedisClient,
) : ActiveTableStateRepository {

    companion object {
        private const val TABLE_KEY_PREFIX = "active_table:"
        private const val SESSION_KEY_PREFIX = "session:"
        private const val LOCK_KEY = "openpoker_table_lock"
        private const val LOCK_EXPIRY_SECONDS = 30L
        private const val LOCK_RETRY_INTERVAL_MS = 50L
    }

    private val json = Json

    private fun tableKey(id: UUID) = "$TABLE_KEY_PREFIX$id"
    private fun sessionKey(sessionId: UUID) = "$SESSION_KEY_PREFIX$sessionId"

    private fun acquireLock(): String {
        val lockValue = UUID.randomUUID().toString()
        val params = SetParams().nx().ex(LOCK_EXPIRY_SECONDS)
        while (true) {
            try {
                val result = redisClient.set(LOCK_KEY, lockValue, params)
                if (result != null) {
                    return lockValue
                }
            } catch (e: Exception) {
                // Lock acquisition failed, retry
            }
            Thread.sleep(LOCK_RETRY_INTERVAL_MS)
        }
    }

    private fun releaseLock(lockValue: String) {
        try {
            val script = """
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("del", KEYS[1])
                else
                    return 0
                end
            """.trimIndent()
            redisClient.eval(script, 1, LOCK_KEY, lockValue)
        } catch (e: Exception) {
            logger.error("Failed to release lock", e)
        }
    }

    override fun getActiveTables(): List<ActiveTable> {
        val lockValue = acquireLock()
        return try {
            val keys = redisClient.keys("$TABLE_KEY_PREFIX*")
            keys.mapNotNull { key ->
                try {
                    json.decodeFromString<ActiveTable>(redisClient.get(key)!!)
                } catch (e: Exception) {
                    logger.error("Failed to deserialize ActiveTable from key $key", e)
                    null
                }
            }
        } finally {
            releaseLock(lockValue)
        }
    }

    override suspend fun performedLockedFunctionOnTables(work: suspend (List<ActiveTable>) -> Unit) {
        val lockValue = acquireLock() ?: throw IllegalStateException("Failed to acquire lock")
        try {
            val keys = redisClient.keys("$TABLE_KEY_PREFIX*")
            val tables = keys.mapNotNull { key ->
                try {
                    json.decodeFromString<ActiveTable>(redisClient.get(key)!!)
                } catch (e: Exception) {
                    logger.error("Failed to deserialize ActiveTable from key $key", e)
                    null
                }
            }
            work(tables)
        } finally {
            releaseLock(lockValue)
        }
    }

    override fun get(id: UUID): ActiveTable? {
        val lockValue = acquireLock() ?: throw IllegalStateException("Failed to acquire lock")
        return try {
            val data = redisClient.get(tableKey(id))
            if (data == null) {
                null
            } else {
                try {
                    json.decodeFromString<ActiveTable>(data)
                } catch (e: Exception) {
                    logger.error("Failed to deserialize ActiveTable for id $id", e)
                    null
                }
            }
        } finally {
            releaseLock(lockValue)
        }
    }

    override suspend fun get(id: UUID, work: suspend (ActiveTable) -> Unit): Table? {
        // TODO: [low] scope this lock to the table
        val lockValue = acquireLock()
        try {
            val data = redisClient.get(tableKey(id))
            if (data != null) {
                val table = json.decodeFromString<ActiveTable>(data)
                work(table)
                return table.table
            } else {
                return null
            }
        } finally {
            releaseLock(lockValue)
        }

    }

    override fun getSession(sessionId: UUID): ActiveTable? {
        val lockValue = acquireLock() ?: throw IllegalStateException("Failed to acquire lock")
        return try {
            val tableIdStr = redisClient.get(sessionKey(sessionId)) ?: return null
            val tableId = try {
                UUID.fromString(tableIdStr)
            } catch (e: Exception) {
                logger.error("Invalid tableId in session mapping: $tableIdStr", e)
                redisClient.del(sessionKey(sessionId))
                return null
            }
            getByKey(tableId)
        } finally {
            releaseLock(lockValue)
        }
    }

    private fun getByKey(id: UUID): ActiveTable? {
        val data = redisClient.get(tableKey(id))
        if (data == null) {
            return null
        } else {
            return try {
                json.decodeFromString<ActiveTable>(data)
            } catch (e: Exception) {
                logger.error("Failed to deserialize ActiveTable for id $id", e)
                null
            }
        }
    }

    override fun set(
        id: UUID,
        table: ActiveTable,
        withLock: Boolean,
    ) {
        val lockValue = if (withLock) {
            acquireLock() ?: throw IllegalStateException("Failed to acquire lock")
        } else null
        try {
            try {
                redisClient.set(tableKey(id), json.encodeToString(table))
                if (!table.finished) {
                    table.playerSockets.forEach { socket ->
                        redisClient.set(sessionKey(socket.sessionId), id.toString())
                    }
                } else {
                    table.playerSockets.forEach { socket ->
                        redisClient.del(sessionKey(socket.sessionId))
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to persist ActiveTable for id $id", e)
            }
        } finally {
            lockValue?.let { releaseLock(it) }
        }
    }
}