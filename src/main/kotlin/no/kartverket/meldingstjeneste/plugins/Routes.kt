package no.kartverket.meldingstjeneste.plugins

import com.kartverket.microsoft.microsoftRoutes
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.kartverket.meldingstjeneste.auth.AUTH_JWT
import no.kartverket.meldingstjeneste.microsoft.MicrosoftService
import no.kartverket.meldingstjeneste.routes.orderRoutes
import no.kartverket.meldingstjeneste.service.OrderService

fun Application.configureRouting(
    orderService: OrderService,
    microsoftService: MicrosoftService,
) {
    routing {
        route("swagger") {
            swaggerUI("/api.json")
        }
        route("api.json") {
            openApi()
        }
        authenticate(AUTH_JWT, strategy = AuthenticationStrategy.Required) {
            microsoftRoutes(microsoftService)
            orderRoutes(orderService)
        }
    }
}
