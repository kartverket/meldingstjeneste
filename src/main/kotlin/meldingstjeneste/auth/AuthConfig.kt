package meldingstjeneste.auth

data class AuthConfig(
    val superUserGroupId: String,
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
                superUserGroupId = getConfigFromEnvOrThrow("SUPER_USER_GROUP_ID"),
                tenantId = tenantId,
                clientId = clientId,
                jwksUri = "https://login.microsoftonline.com/$tenantId/discovery/v2.0/keys",
                issuer = "https://login.microsoftonline.com/$tenantId/v2.0"
            )
        }
    }
}

