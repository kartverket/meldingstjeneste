package meldingstjeneste.plugins

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.ktor.server.application.Application
import io.ktor.server.application.install
import meldingstjeneste.port

fun Application.configureSwagger() {
    install(SwaggerUI) {
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
