package app

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import openpoker.v1.TableServiceWireGrpc
import server.GrpcTableServiceServer
import java.util.concurrent.TimeUnit
import openpoker.v1.TournamentServiceWireGrpc
import server.GrpcTournamentServiceServer

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 50051
    val server = NettyServerBuilder.forPort(port)
        .addService(TableServiceWireGrpc.BindableAdapter(service = ::GrpcTableServiceServer))
        .addService(TournamentServiceWireGrpc.BindableAdapter(service = ::GrpcTournamentServiceServer))
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
