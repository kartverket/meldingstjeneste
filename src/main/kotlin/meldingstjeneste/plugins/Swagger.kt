package meldingstjeneste.plugins

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.ktor.server.application.*

fun Application.configureSwagger() {
    install(SwaggerUI) {
        info {
            title = "Kartverkets meldingstjeneste"
            version = "0.0.1"
        }
        server {
            url = "http://localhost:8080"
            description = "Local"
        }
        tags {
            tag("Autentisering") {
                description = "Endepunkter for autentisering og håndtering av tokens"
            }
            tag("Varslinger") {
                description = "Endepunkter for utsending av varsler via Altinns varslingstjeneste"
            }
            tag("Altinn Proxy") {
                description = "Endepunkter for videresending av kall til Altinns varslingstjeneste"
            }
        }
    }
}
