package meldingstjeneste.plugins

import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import meldingstjeneste.routes.orderRoutes
import meldingstjeneste.routes.proxyRoutes
import meldingstjeneste.service.OrderService
import meldingstjeneste.service.ProxyService

fun Application.configureRouting(
    orderService: OrderService,
    proxyService: ProxyService,
) {
    routing {
        route("swagger") {
            swaggerUI("/api.json")
        }
        route("api.json") {
            openApiSpec()
        }
        get("/actuator/health") {
            call.respondText( "OK")
        }
        orderRoutes(orderService)
        proxyRoutes(proxyService)

    }
}
