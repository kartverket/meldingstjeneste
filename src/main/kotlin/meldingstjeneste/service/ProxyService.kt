package meldingstjeneste.service

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

// TODO: Denne kan antageligvis slettes etter hvert
class ProxyService {
    private val client = HttpClientProvider.client

    suspend fun forwardGet(path: String): HttpResponse = client.get(path)

    suspend fun forwardPost(
        path: String,
        body: String,
    ): HttpResponse =
        client.post(path) {
            setBody(body)
            contentType(ContentType.Application.Json)
        }

    suspend fun forwardPut(
        path: String,
        body: String,
    ): HttpResponse =
        client.put(path) {
            setBody(body)
            contentType(ContentType.Application.Json)
        }
}
