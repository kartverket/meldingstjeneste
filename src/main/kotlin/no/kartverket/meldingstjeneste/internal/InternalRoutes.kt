package no.kartverket.meldingstjeneste.internal

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.internalRoutes(metricsRegistry: PrometheusMeterRegistry) {
    routing {
        route("/actuator/health") {
            get {
                call.respondText("OK")
            }
        }

        route("/actuator/metrics") {
            get {
                call.respondText(metricsRegistry.scrape())
            }
        }
    }
}
