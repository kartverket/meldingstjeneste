package no.kartverket.meldingstjeneste.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.kartverket.meldingstjeneste.env
import no.kartverket.meldingstjeneste.model.Jwk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.RSAPrivateKeySpec
import java.util.Base64
import java.util.Date


class TokenService {
    val logger: Logger = LoggerFactory.getLogger(TokenService::class.java)

    val altinnBaseUrl: String = env["ALTINN_BASE_URL"]
    val maskinportenBaseUrl: String = env["MASKINPORTEN_BASE_URL"]

    suspend fun getAccessToken(): String =
        exchangeAccessToken(
            getMaskinportenToken("notifications"),
        )

    private suspend fun getMaskinportenToken(apiProvider: String): String {
        logger.info("Fetching Maskinporten token for $apiProvider")

        val client = HttpClient()
        try {
            val jwt = getSignedJWT(apiProvider)
            val response: HttpResponse =
                client.post("$maskinportenBaseUrl/token") {
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
                throw RuntimeException("Failed to get access token from Maskinporten - ${response.status.value} - ${response.bodyAsText()}")
            }

            // Parse the response body as a String
            val responseBody: String = response.bodyAsText()

            if (responseBody.isBlank()) {
                throw RuntimeException("Empty response body from Maskinporten")
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

    private suspend fun exchangeAccessToken(accessTokenMaskinporten: String): String {
        logger.info("Exchanging access token from Maskinporten to Altinn")

        val client = HttpClient()
        try {
            val response: HttpResponse =
                client.get("$altinnBaseUrl/authentication/api/v1/exchange/maskinporten?test=false") {
                    header(HttpHeaders.Authorization, "Bearer $accessTokenMaskinporten")
                }

            if (!response.status.isSuccess()) {
                throw Exception("Failed to get access token from token exchange with Altinn - ${response.status.value} - ${response.bodyAsText()}")
            }

            val altinnToken = response.body<String>()
            if (altinnToken.isEmpty()) {
                throw Exception("Empty response body from exchange access token")
            }
            return altinnToken
        } catch (e: Exception) {
            logger.error("Failed to get access token from altinn", e)
            throw e
        } finally {
            client.close()
        }
    }

    private fun getSignedJWT(apiProvider: String): String {
        val clientId: String
        val jwkJson: String
        val claimValue: String

        when (apiProvider) {
            "notifications" -> {
                clientId = env["MASKINPORTEN_CLIENT_ID"]
                jwkJson = env["MASKINPORTEN_CLIENT_JWK"]
                claimValue = "altinn:serviceowner/notifications.create"
            }

            else -> throw IllegalArgumentException("Invalid type: $apiProvider")
        }

        if (clientId.isBlank() || jwkJson.isBlank()) {
            throw NullPointerException("Client Id or JWK must be set in environment variables")
        }

        val jwk: Jwk = try {
            Json.decodeFromString(jwkJson)
        } catch (e: Exception) {
            logger.error("Failed to parse JWK JSON: $jwkJson", e)
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
                .withAudience("$maskinportenBaseUrl/")
                .withIssuedAt(Date())
                .withClaim(
                    "scope",
                    claimValue,
                )
                .withKeyId(jwk.kid)
                .withExpiresAt(Date(System.currentTimeMillis() + 2 * 60 * 1000)) // 2 minutes expiry
                .sign(algorithm)
        return token
    }
}
