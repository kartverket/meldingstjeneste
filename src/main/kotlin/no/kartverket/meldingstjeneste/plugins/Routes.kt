package no.kartverket.meldingstjeneste.plugins

import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.kartverket.meldingstjeneste.auth.AUTH_JWT
import no.kartverket.meldingstjeneste.auth.AuthConfig
import no.kartverket.meldingstjeneste.routes.authConfigRoute
import no.kartverket.meldingstjeneste.routes.eFormidlingroutes
import no.kartverket.meldingstjeneste.routes.orderRoutes
import no.kartverket.meldingstjeneste.service.OrderService
import no.kartverket.meldingstjeneste.service.eFormidlingService

fun Application.configureRouting(
    orderService: OrderService,
    eFormidlingService: eFormidlingService,
    authConfig: AuthConfig,
) {
    routing {
        route("swagger") {
            swaggerUI("/api.json")
        }
        route("api.json") {
            openApi()
        }
        route("/api") {
            authConfigRoute(authConfig)
            authenticate(AUTH_JWT, strategy = AuthenticationStrategy.Required) {
                orderRoutes(orderService)
                eFormidlingroutes(eFormidlingService)
            }
        }
    }
}
