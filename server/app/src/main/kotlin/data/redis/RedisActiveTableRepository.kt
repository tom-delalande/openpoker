package data.redis

import app.logger
import domain.model.Table
import domain.table.ActiveTable
import domain.table.ActiveTableStateRepository
import java.time.Duration
import kotlinx.serialization.json.Json
import java.util.UUID
import java.time.Instant
import kotlin.math.exp
import redis.clients.jedis.RedisClient
import redis.clients.jedis.params.SetParams

class RedisActiveTableRepository(
    private val redisClient: RedisClient,
) : ActiveTableStateRepository {

    companion object {
        private const val TABLE_KEY_PREFIX = "active_table:"
        private const val SESSION_KEY_PREFIX = "session:"
        private const val LOCK_KEY_PREFIX = "openpoker_table_lock:"
        private const val LOCK_EXPIRY_SECONDS = 30L
        private const val LOCK_RETRY_INTERVAL_MS = 50L
    }

    private val json = Json

    private fun tableKey(id: UUID) = "$TABLE_KEY_PREFIX$id"
    private fun sessionKey(sessionId: UUID) = "$SESSION_KEY_PREFIX$sessionId"
    private fun lockKey(id: UUID) = "$LOCK_KEY_PREFIX$id"

    private fun acquireLock(tableId: UUID): String {
        val lockValue = UUID.randomUUID().toString()
        val params = SetParams().nx().ex(LOCK_EXPIRY_SECONDS)
        while (true) {
            try {
                val result = redisClient.set(lockKey(tableId), lockValue, params)
                if (result != null) {
                    return lockValue
                }
            } catch (e: Exception) {
                logger.warn("Failed to acquire lock!", e)
            }
            Thread.sleep(LOCK_RETRY_INTERVAL_MS)
        }
    }

    private fun releaseLock(tableId: UUID, lockValue: String) {
        try {
            val script = """
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("del", KEYS[1])
                else
                    return 0
                end
            """.trimIndent()
            redisClient.eval(script, 1, lockKey(tableId), lockValue)
        } catch (e: Exception) {
            logger.error("Failed to release lock", e)
        }
    }

    override suspend fun get(id: UUID, work: suspend (ActiveTable) -> ActiveTable): Table? {
        val lockValue = acquireLock(id)
        try {
            val data = redisClient.get(tableKey(id))
            if (data != null) {
                val table = json.decodeFromString<ActiveTable>(data)
                val updated = work(table)
                set(id, updated)
                return updated.table
            } else {
                return null
            }
        } finally {
            releaseLock(id, lockValue)
        }

    }

    override suspend fun create(id: UUID, table: ActiveTable) {
        set(id, table)
    }

    override fun getSession(sessionId: UUID): UUID? {
        val tableIdStr = redisClient.get(sessionKey(sessionId)) ?: return null
        return try {
            UUID.fromString(tableIdStr)
        } catch (e: Exception) {
            logger.error("Invalid tableId in session mapping: $tableIdStr", e)
            redisClient.del(sessionKey(sessionId))
            null
        }
    }

    private fun set(
        id: UUID,
        table: ActiveTable,
    ) {
        try {
            redisClient.set(tableKey(id), json.encodeToString(table))
            // TODO: check this is necessary
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
    }
}