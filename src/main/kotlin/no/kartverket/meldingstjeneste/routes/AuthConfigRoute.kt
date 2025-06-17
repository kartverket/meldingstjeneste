package no.kartverket.meldingstjeneste.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import no.kartverket.meldingstjeneste.auth.AuthConfig
import no.kartverket.meldingstjeneste.env

@Serializable
data class AuthConfigResponse(
    val AZURE_APP_CLIENT_ID: String,
    val AZURE_APP_AUTHORITY: String,
    val AZURE_APP_LOGIN_REDIRECT_URI: String,
)

fun Route.authConfigRoute(authConfig: AuthConfig) {
    get("/authConfig") {
        call.respond(
            AuthConfigResponse(
                authConfig.clientId,
                "https://login.microsoftonline.com/${authConfig.tenantId}",
                AZURE_APP_LOGIN_REDIRECT_URI = env["FRONTEND_INGRESS"],
            )
        )
    }
}
