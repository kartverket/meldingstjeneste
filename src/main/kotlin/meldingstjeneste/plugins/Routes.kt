package meldingstjeneste.plugins

import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import meldingstjeneste.routes.orderRoutes
import meldingstjeneste.service.OrderService

fun Application.configureRouting(
    orderService: OrderService,
) {
    routing {
        route("swagger") {
            swaggerUI("/api.json")
        }
        route("api.json") {
            openApiSpec()
        }
        orderRoutes(orderService)
    }
}
