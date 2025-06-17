package no.kartverket.meldingstjeneste.auth

import no.kartverket.meldingstjeneste.env

data class AuthConfig(
    val tenantId: String,
    val clientId: String,
    val jwksUri: String,
    val issuer: String,
) {
    companion object {
        fun load(): AuthConfig {
            val tenantId = getConfigFromEnvOrThrow("TENANT_ID")
            val clientId = getConfigFromEnvOrThrow("CLIENT_ID")
            return AuthConfig(
                tenantId = tenantId,
                clientId = clientId,
                jwksUri = "https://login.microsoftonline.com/$tenantId/discovery/v2.0/keys",
                issuer = "https://login.microsoftonline.com/$tenantId/v2.0",
            )
        }
    }
}

fun getConfigFromEnvOrThrow(variableName: String): String =
    env[variableName]
        ?: throw IllegalStateException("Unable to initialize app config, \"$variableName\" is null")
