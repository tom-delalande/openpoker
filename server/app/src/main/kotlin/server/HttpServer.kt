package server

import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket

fun httpServer() {
    embeddedServer(Netty, port = 9090) {
        routing {
            route("/api") {
                gameEndpoints()
                tableEndpoints()
            }
        }
    }.start(wait = true)
}

fun Route.gameEndpoints() {
    route("/game") {

    }
}

fun Route.tableEndpoints() {
    route("/table") {
        get("/ws") {
            val token = call.request.queryParameters["token"]
            val gameId = call.request.queryParameters["gameId"]

        }
        webSocket("/{id}") {

        }
    }
}
