package app

import data.inmemory.InMemoryActiveTableStateRepository
import data.inmemory.InMemoryHandHistoryRepository
import domain.table.TableService
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import openpoker.v1.TableServiceWireGrpc
import server.GrpcTableServiceServer
import java.util.concurrent.TimeUnit
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import openpoker.v1.TournamentServiceWireGrpc
import server.GrpcGameServiceServer

suspend fun main() {
    val activeTableStateRepository = InMemoryActiveTableStateRepository()
    val handHistoryRepository = InMemoryHandHistoryRepository()
    val tableService = TableService(activeTableStateRepository, handHistoryRepository)
    coroutineScope {
        launch {
            while (true) {
                tableService.process(Instant.now())
                delay(500.milliseconds)
            }
        }
    }

    val port = System.getenv("PORT")?.toIntOrNull() ?: 50051
    val server = NettyServerBuilder.forPort(port)
        .addService(TableServiceWireGrpc.BindableAdapter(service = ::GrpcTableServiceServer))
        .addService(TournamentServiceWireGrpc.BindableAdapter(service = ::GrpcGameServiceServer))
        .build()
        .start()

    println("gRPC server listening on port $port")
    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.shutdown()
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow()
            }
        },
    )
    server.awaitTermination()
}
