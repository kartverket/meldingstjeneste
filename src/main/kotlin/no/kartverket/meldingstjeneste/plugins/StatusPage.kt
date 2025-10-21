package no.kartverket.meldingstjeneste.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.utils.io.InternalAPI
import no.kartverket.meldingstjeneste.logger

@OptIn(InternalAPI::class)
fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString())
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message.toString())
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, "You don't have permission to access this resource. ${cause.message}")
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, "You don't have permission to access this endpoint. ${cause.message}")
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, "The order-ID provided does not exist. ${cause.message}")
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, "${cause.rootCause?.message}")
        }
        exception<Throwable> { call, cause ->
            logger.error("Noe gikk galt", cause)
            call.respond(HttpStatusCode.InternalServerError, "\n ERROR: $cause \n")
        }
    }
}

class UnauthorizedException(
    message: String,
) : RuntimeException(message)

class ForbiddenException(
    message: String,
) : RuntimeException(message)
