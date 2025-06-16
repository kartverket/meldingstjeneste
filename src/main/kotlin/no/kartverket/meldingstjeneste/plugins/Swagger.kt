package no.kartverket.meldingstjeneste.plugins

import io.github.smiley4.ktoropenapi.OpenApi
import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.kartverket.meldingstjeneste.port

fun Application.configureSwagger() {
    install(OpenApi) {
        info {
            title = "Kartverkets meldingstjeneste"
            version = "0.0.1"
        }
        server {
            url = "http://localhost:${port}"
            description = "Local"
        }
        tags {
            tag("Varslinger") {
                description = "Endepunkter for utsending av varsler via Altinns varslingstjeneste"
            }
        }
    }
}
