package no.kartverket.meldingstjeneste.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import java.net.URI
import java.util.concurrent.TimeUnit

const val AUTH_JWT = "auth-jwt"

fun Application.configureAuth(authConfig: AuthConfig) {

    val jwkProvider =
        JwkProviderBuilder(URI(authConfig.jwksUri).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = "Meldingstjeneste"
            verifier(jwkProvider, authConfig.issuer) {
                withIssuer(authConfig.issuer)
                withAudience(authConfig.clientId)
                acceptLeeway(3)
            }

            validate { jwtCredential ->
                JWTPrincipal(jwtCredential.payload)
            }
        }
    }
}

fun ApplicationCall.getUserId(): String? {
    val userid = this.principal<JWTPrincipal>()?.payload?.getClaim("oid")?.asString() ?: return null
    return userid
}
