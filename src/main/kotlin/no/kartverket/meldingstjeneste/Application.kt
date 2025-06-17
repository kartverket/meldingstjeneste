package no.kartverket.meldingstjeneste

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
import kotlinx.serialization.json.Json
import no.kartverket.meldingstjeneste.auth.AuthConfig
import no.kartverket.meldingstjeneste.auth.configureAuth
import no.kartverket.meldingstjeneste.internal.Metrics
import no.kartverket.meldingstjeneste.internal.internalRoutes
import no.kartverket.meldingstjeneste.plugins.configureRouting
import no.kartverket.meldingstjeneste.plugins.configureStatusPage
import no.kartverket.meldingstjeneste.plugins.configureSwagger
import no.kartverket.meldingstjeneste.plugins.configureValidation
import no.kartverket.meldingstjeneste.service.OrderService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val env =
    dotenv {
        ignoreIfMissing = true
        systemProperties = true
    }

val logger: Logger = LoggerFactory.getLogger(object {}::class.java)
val environment: String = env["ENVIRONMENT"]
val port = if (environment == "localhost") 8081 else 8080

fun main() {
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    logger.info("Starting app..")
    val authConfig = AuthConfig.load()
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
        allowHost(env["FRONTEND_INGRESS"].removePrefix("http://"))
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Put)
    }

    configureAuth(authConfig)

    val orderService = OrderService() // Ensure this is initialized properly
    configureSwagger()
    configureRouting(orderService)
    configureValidation()
    configureStatusPage()
    internalRoutes(metricsRegistry)
}
