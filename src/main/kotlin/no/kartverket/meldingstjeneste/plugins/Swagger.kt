package no.kartverket.meldingstjeneste.plugins

import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.ktor.server.application.Application
import io.ktor.server.application.install

fun Application.configureSwagger() {
    install(OpenApi) {
        info {
            title = "Kartverkets meldingstjeneste"
            version = "0.0.1"
        }
        tags {
            tag("Varslinger") {
                description = "Endepunkter for utsending av varsler via Altinns varslingstjeneste"
            }
            tag("eFormidling") {
                description = "Endepunkter for utsending av meldinger via eFormidling"
            }
        }
        security {
            securityScheme("entra") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
                bearerFormat = "jwt"
            }
        }
    }
}
