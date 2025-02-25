package meldingstjeneste.service

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import meldingstjeneste.service.OrderService.Companion.BASE_URL

object HttpClientProvider {
    val client: HttpClient by lazy {
        HttpClient {
            defaultRequest {
                url(BASE_URL)
                bearerAuth(updateAccessToken())
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        encodeDefaults = false
                        ignoreUnknownKeys = true
                    },
                )
            }
        }
    }

    private fun updateAccessToken(): String =
        runBlocking {
            TokenManager.getToken()
        }
}
