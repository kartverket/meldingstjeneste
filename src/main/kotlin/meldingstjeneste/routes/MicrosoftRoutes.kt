package com.kartverket.microsoft

import io.ktor.http.*
 import io.ktor.server.response.*
import io.ktor.server.routing.*
import meldingstjeneste.auth.getUserId
import meldingstjeneste.microsoft.MicrosoftService

fun Route.microsoftRoutes(microsoftService: MicrosoftService) {
    route("/microsoft") {
        route("/me") {
            route("/teams") {
                get {
                    val userId = call.getUserId() ?: run {
                        call.respond(HttpStatusCode.Forbidden)
                        return@get
                    }
                    val groups = microsoftService.getMemberGroups(userId)
                    call.respond(groups)
                }
            }
        }
    }
}