package no.kartverket.meldingstjeneste

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() =
        testApplication {
            val response = client.get("/")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
}
