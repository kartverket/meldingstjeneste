package no.kartverket.meldingstjeneste.service

import io.ktor.client.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.kartverket.meldingstjeneste.logger


object HttpClientProvider {
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    encodeDefaults = false
                    ignoreUnknownKeys = true
                },
            )
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val accessToken = runBlocking {
                        TokenService().getAccessToken()
                    }
                    logger.info("Setting access token for client")
                    BearerTokens(accessToken, "")
                }
                refreshTokens {
                    val accessToken = runBlocking {
                        TokenService().getAccessToken()
                    }
                    logger.info("Fetching new access token due to 401 from Altinn")
                    BearerTokens(accessToken, "")
                }
            }
        }
    }
}
