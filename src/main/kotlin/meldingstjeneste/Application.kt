@file:Suppress("ktlint:standard:no-wildcard-imports")

package meldingstjeneste

import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import meldingstjeneste.internal.Metrics
import meldingstjeneste.internal.internalRoutes
import meldingstjeneste.plugins.configureRouting
import meldingstjeneste.plugins.configureStatusPage
import meldingstjeneste.plugins.configureSwagger
import meldingstjeneste.plugins.configureValidation
import meldingstjeneste.service.OrderService
import meldingstjeneste.service.ProxyService

fun main() {
    val environment = env["ENVIRONMENT"]
    val port = if (environment == "localhost") 8081 else 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

val env =
    dotenv {
        ignoreIfMissing = true
        systemProperties = true
    }

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    val metricsRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        val logbackMetrics = LogbackMetrics()
        logbackMetrics.bindTo(metricsRegistry)

        Metrics.init(metricsRegistry)

        registry = metricsRegistry
    }

    install(ContentNegotiation) {
        json(
            Json {
                explicitNulls = false // omit properties that are `null`
                ignoreUnknownKeys = true
            },
        )
    }

    install(CORS) {
        allowHost("localhost:3001")
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Put)
    }

    val orderService = OrderService() // Ensure this is initialized properly
    val proxyService = ProxyService() // Ensure this is initialized properly
    configureSwagger()
    configureRouting(orderService, proxyService)
    configureValidation()
    configureStatusPage()
    internalRoutes(metricsRegistry)
}
