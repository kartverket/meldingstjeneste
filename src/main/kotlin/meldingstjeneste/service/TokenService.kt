package meldingstjeneste.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import meldingstjeneste.env
import meldingstjeneste.model.Jwk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.RSAPrivateKeySpec
import java.util.Base64
import java.util.Date

object TokenManager {
    private var token: String? = null
    private val tokenService: TokenService = TokenService()
    private var tokenExpiration: Date? = null

    init {
        runBlocking {
            refreshToken()
        }
    }

    suspend fun getToken(): String {
        val now = Date()
        if (token == null || tokenExpiration == null || now.after(tokenExpiration)) {
            refreshToken()
        }
        return token!!
    }

    private suspend fun refreshToken() {
        token = tokenService.getAccessToken()
        tokenExpiration = JWT.decode(token).expiresAt
    }
}

class TokenService {
    val logger: Logger = LoggerFactory.getLogger(TokenService::class.java)

    suspend fun exchangeAccessToken(accessTokenMaskinporten: String): String {
        val client = HttpClient()
        try {
            val response: HttpResponse =
                client.get("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten?test=false") {
                    header(HttpHeaders.Authorization, "Bearer $accessTokenMaskinporten")
                }

            if (!response.status.isSuccess()) {
                throw IOException("Failed to get access token from token exchange with Altinn: HTTP ${response.status.value}")
            }

            val altinnToken = response.body<String>()
            if (altinnToken.isEmpty()) {
                throw IOException("Empty response body from exchange access token")
            }
            return altinnToken
        } catch (e: Exception) {
            throw e
        } finally {
            client.close()
        }
    }

    suspend fun getAccessToken(): String =
        exchangeAccessToken(
            getMaskinportenToken("notifications"),
        )

    suspend fun getKrrAccessToken(): String = getMaskinportenToken("krr")

    suspend fun getMaskinportenToken(apiProvider: String): String {
        val client = HttpClient()
        try {
            val jwt = getSignedJWT(apiProvider)
            val response: HttpResponse =
                client.post("https://test.maskinporten.no/token") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        Parameters
                            .build {
                                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                                append("assertion", jwt)
                            }.formUrlEncode(),
                    )
                }

            if (!response.status.isSuccess()) {
                throw IOException("Failed to get access token from Maskinporten: HTTP ${response.status.value}")
            }

            // Parse the response body as a String
            val responseBody: String = response.bodyAsText()

            if (responseBody.isBlank()) {
                throw IOException("Empty response body from Maskinporten")
            }

            // Parse the JSON response
            val jsonResponse: JsonElement = Json.parseToJsonElement(responseBody)

            val accessToken =
                jsonResponse.jsonObject["access_token"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Expected non-empty response body but received empty.")

            return accessToken
        } catch (e: Exception) {
            logger.error("Failed to get access token from Maskinporten", e)
            throw e
        } finally {
            client.close() // Ensure the client is closed to free resources
        }
    }

    private fun getSignedJWT(apiProvider: String): String {
        val clientId: String
        val jwkJson: String
        val claimValue: String
        val keyId: String

        when (apiProvider) {
            "notifications" -> {
                clientId = env["ALTINN_CLIENT_ID"]
                jwkJson = env["ALTINN_JWK"]
                claimValue = "altinn:serviceowner/notifications.create"
                keyId = "kart_melding_test"
            }
            "krr" -> {
                clientId = env["KRR_CLIENT_ID"]
                jwkJson = env["KRR_JWK"]
                claimValue = "krr:global/kontaktinformasjon.read krr:global/digitalpost.read"
                keyId = "kart_melding_krr_test"
            }
            else -> throw IllegalArgumentException("Invalid type: $apiProvider")
        }

        if (clientId.isNullOrBlank() || jwkJson.isNullOrBlank()) {
            throw NullPointerException("Client Id or JWK must be set in environment variables")
        }

        val jwk: Jwk = try {Json.decodeFromString(jwkJson)} catch (e: Exception) {
            logger.error("Failed to parse JWK JSON: $jwkJson")
            throw IllegalArgumentException("Failed to parse JWK JSON: $jwkJson")
        }

        val modulus = jwk.n
        val privateExponent = jwk.d

        val modulusBigInt = BigInteger(1, Base64.getUrlDecoder().decode(modulus))
        val privateExponentBigInt = BigInteger(1, Base64.getUrlDecoder().decode(privateExponent))

        val spec = RSAPrivateKeySpec(modulusBigInt, privateExponentBigInt)
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(spec) as RSAPrivateKey

        val algorithm = Algorithm.RSA256(null, privateKey)
        val token =
            JWT
                .create()
                .withIssuer(clientId)
                .withAudience("https://test.maskinporten.no/")
                .withIssuedAt(Date())
                .withClaim(
                    "scope",
                    claimValue,
                ).withKeyId(keyId)
                .withExpiresAt(Date(System.currentTimeMillis() + 2 * 60 * 1000)) // 2 minutes expiry
                .sign(algorithm)
        return token
    }
}
