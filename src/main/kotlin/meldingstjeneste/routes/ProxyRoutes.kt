package meldingstjeneste.routes

import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.github.smiley4.ktorswaggerui.dsl.routing.route
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import meldingstjeneste.service.ProxyService

// TODO: Disse bør slettes
fun Route.proxyRoutes(proxyService: ProxyService) {
    route("/notifications/api/v1") {
        get("/{...}", altinnProxyDoc(HttpMethod.Get)) {
            val dest = call.request.uri
            val response = proxyService.forwardGet(dest)
            call.respond(response.status, response.bodyAsText())
        }

        post("/{...}", altinnProxyDoc(HttpMethod.Post)) {
            val dest = call.request.uri
            val body = call.receiveText()
            val response = proxyService.forwardPost(dest, body)
            call.response.headers.append(HttpHeaders.ContentType, "application/json")
            call.respond(response.status, response.bodyAsText())
        }

        put("/{...}", altinnProxyDoc(HttpMethod.Put)) {
            val dest = call.request.uri
            val body = call.receiveText()
            val response = proxyService.forwardPut(dest, body)
            call.response.headers.append(HttpHeaders.ContentType, "application/json")
            call.respond(response.status, response.bodyAsText())
        }
    }
}

private fun altinnProxyDoc(method: HttpMethod): OpenApiRoute.() -> Unit =
    {
        tags("Altinn Proxy")
        summary = "Proxy"
        description =
            "Proxy for å videresende ${method.value}-kall til Altinn sitt varslings API. " +
            "Dette endepunktet håndterer autentisering og videresender forespørselen til Altinn. \n\n" +
            "For detaljer om Altinns API og responser, se [Altinn OpenAPI-dokumentasjon](https://docs.altinn.studio/notifications/reference/api/openapi/#/)."
        response {
            default {
                description = "Respons fra Altinn. Se Altinns dokumentasjon for detaljer om spesifikke responstyper og statuskoder."
                body<Any> {
                    description = "Responsen fra Altinn kan variere avhengig av hvilket API-endepunkt som kalles"
                }
            }
        }
    }
