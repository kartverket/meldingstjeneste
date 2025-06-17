package no.kartverket.meldingstjeneste.auth

import no.kartverket.meldingstjeneste.env

class EntraConfig(
    val tenantId: String,
    val clientId: String,
    val clientSecret: String,
) {
    companion object {
        fun load(): EntraConfig = EntraConfig(
            tenantId = getConfigFromEnvOrThrow("TENANT_ID"),
            clientId = getConfigFromEnvOrThrow("CLIENT_ID"),
            clientSecret = getConfigFromEnvOrThrow("CLIENT_SECRET")
        )
    }
}

fun getConfigFromEnvOrThrow(variableName: String): String =
    env[variableName]
        ?: throw IllegalStateException("Unable to initialize app config, \"$variableName\" is null")
