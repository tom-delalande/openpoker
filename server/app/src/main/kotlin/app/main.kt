package app

import data.inmemory.InMemoryActiveTableStateRepository
import data.inmemory.InMemoryAuthRepository
import data.inmemory.InMemoryCashGameRepository
import data.inmemory.InMemoryHandHistoryRepository
import data.postgres.PostgresAuthRepository
import data.postgres.PostgresCashGameRepository
import data.postgres.PostgresHandHistoryRepository
import data.redis.RedisActiveTableRepository
import domain.table.ActiveTableStateRepository
import domain.table.HandHistoryRepository
import domain.table.TableService
import domain.tournament.CashGameRepository
import domain.tournament.CashGameService
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import redis.clients.jedis.RedisClient
import server.AuthRepository
import server.authEndpoints
import server.gameEndpoints
import server.models.HandEvent
import server.tableEndpoints

val logger = LoggerFactory.getLogger("Main")

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val useInMemory = System.getenv("IN_MEMORY_ENABLED")?.toBoolean() ?: false

    val (activeTableRepository, handHistoryRepository, cashGameRepository, authRepository) = if (useInMemory) {
        Repositories(
            InMemoryActiveTableStateRepository(),
            InMemoryHandHistoryRepository(),
            InMemoryCashGameRepository(),
            InMemoryAuthRepository(),
        )
    } else {
        val dataSource = PGSimpleDataSource().apply {
            user = System.getenv("POSTGRES_USER")
            password = System.getenv("POSTGRES_PASSWORD")
            setUrl(System.getenv("POSTGRES_URL"))
        }

        Flyway.configure()
            .locations("filesystem:./db/migrations")
            .dataSource(dataSource)
            .load()
            .migrate()

        val jdbcClient = JdbcClient.create(dataSource)

        val redisUser = System.getenv("REDIS_USER")
        val redisPassword = System.getenv("REDIS_PASSWORD")
        val redisClient = if (redisUser != null && redisPassword != null) {
            RedisClient.create(
                System.getenv("REDIS_HOST"),
                System.getenv("REDIS_PORT")!!.toInt(),
                redisUser,
                redisPassword
            )
        } else {
            RedisClient.create(
                System.getenv("REDIS_HOST"),
                System.getenv("REDIS_PORT")!!.toInt(),
            )
        }
        Repositories(
            RedisActiveTableRepository(redisClient),
            PostgresHandHistoryRepository(jdbcClient),
            PostgresCashGameRepository(jdbcClient),
            PostgresAuthRepository(jdbcClient)
        )

    }
    val websockets = ConcurrentHashMap<UUID, MutableSharedFlow<HandEvent>>()
    val tableService = TableService(activeTableRepository, websockets)
    val gameService = CashGameService(cashGameRepository, handHistoryRepository, tableService)

    CoroutineScope(Dispatchers.Default).launch {
        val logger = LoggerFactory.getLogger("ProcessingThread")
        while (true) {
            try {
                gameService.processTables()
                delay(500.milliseconds)
            } catch (exception: Exception) {
                logger.error("error when processing", exception)
            }
        }
    }

    embeddedServer(Netty, port = 3001) {

        module(authRepository, gameService, tableService, websockets)


    }.start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module(
    authRepository: AuthRepository,
    gameService: CashGameService,
    tableService: TableService,
    websockets: ConcurrentHashMap<UUID, MutableSharedFlow<HandEvent>>,
) {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        anyHost()
    }

    routing {
        staticFiles("/", File("client"))
        authEndpoints(authRepository, gameService)
        gameEndpoints(gameService, authRepository)
        tableEndpoints(websockets, authRepository, tableService)
    }
}

data class Repositories(
    val activeTableStateRepository: ActiveTableStateRepository,
    val handHistoryRepository: HandHistoryRepository,
    val cashGameRepository: CashGameRepository,
    val authRepository: AuthRepository,
)