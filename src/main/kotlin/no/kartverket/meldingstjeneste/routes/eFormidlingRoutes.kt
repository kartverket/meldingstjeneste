package no.kartverket.meldingstjeneste.routes

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import no.kartverket.meldingstjeneste.clients.ConversationDTO
import no.kartverket.meldingstjeneste.clients.Fnr
import no.kartverket.meldingstjeneste.clients.FysiskPerson
import no.kartverket.meldingstjeneste.logger
import no.kartverket.meldingstjeneste.service.EFormidlingService
import no.kartverket.meldingstjeneste.service.MissingDigitalCapabilitiesException


fun Route.eFormidlingroutes(eFormidlingService: EFormidlingService) {
    post("/eFormidling/send/bulk", sendBulkDoc) {
        val (tittel, melding, identifikatorer) = call.receive<MeldingBulkDTO>()

        val mottakere = identifikatorer.map { id ->
            FysiskPerson(
                identifikator = id
            )
        }.toSet()

        logger.info("Sender melding til eFormidling for ${mottakere.size} mottakere")

        call.application.launch {
            var antallSendt = 0
            mottakere.forEach { mottaker ->
                try {
                    val ok = eFormidlingService.sendMelding(mottaker, tittel, melding)

                    if (ok) {
                        antallSendt++
                    }

                } catch (e: Exception) {
                    logger.error("Kunne ikke sende melding", e)
                    throw e
                }

            }
            logger.info("Sendte $antallSendt av ${mottakere.size} meldinger")
        }


        logger.info("Startet med å sende meldinger til eFormidling")
        call.respond("Sender meldinger")
    }

    post("/eFormidling/send/single", sendEnDoc) {
        val (tittel, melding, identifikator) = call.receive<MeldingSingleDTO>()

        val fysiskPerson = FysiskPerson(
            identifikator = identifikator
        )

        try {
            eFormidlingService.sendMelding(fysiskPerson, tittel, melding)
            call.respond(HttpStatusCode.OK)
        }

        catch (e: Exception) {

            when (e) {
                is MissingDigitalCapabilitiesException -> {
                    logger.info("Mottaker har ikke digital capability, sender ikke melding til eFormidling")
                    call.respond(HttpStatusCode.BadRequest, APIErrorResponse(APIErrorType.MOTTAKER_MANGLER_DIGITAL_CAPABILITY,"Bruker mangler digitial capability: ${e.message}"))
                }

                is IllegalArgumentException -> {
                    logger.error("Feil ved sending til eFormidling: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, APIErrorResponse(APIErrorType.KLIENT_FEIL, "Feil ved sending til eFormidling: ${e.message}"))
                }

                else -> {
                    logger.error("Uventet feil ved sending til eFormidling", e)
                    call.respond(HttpStatusCode.InternalServerError, APIErrorResponse(APIErrorType.SERVER_FEIL, "Uventet feil ved sending til eFormidling"))}
            }
        }

    }

    get("/eFormidling/result", resultDoc) {
        val params = call.request.queryParameters
        val datolevert = params["dato"]!!
        val vellykkedeutsendinger = eFormidlingService.hentMottakereMedVellykketLevering(datolevert)
        call.respond(vellykkedeutsendinger)
    }

    get("/eFormidling/result/fnr", resultFnrDoc) {
        val params = call.request.queryParameters
        val datolevert = params["dato"]!!
        val vellykkedeutsendinger = eFormidlingService.hentMottakereMedVellykketLevering(datolevert)
            .map { conversationDTO -> conversationDTO.receiver }.toSet()

        call.respond(vellykkedeutsendinger)
    }
}

@Serializable
data class MeldingBulkDTO(
    val tittel: String,
    val melding: String,
    val identifikatorer: List<String>,
)

@Serializable
data class MeldingSingleDTO(
    val tittel: String,
    val melding: String,
    val identifikator: String,
)

@Serializable
data class APIErrorResponse(
    val error: APIErrorType,
    val message: String,
)

@Serializable
enum class APIErrorType() {
    SERVER_FEIL,
    KLIENT_FEIL,
    MOTTAKER_MANGLER_DIGITAL_CAPABILITY,
}

private val sendBulkDoc: RouteConfig.() -> Unit = {
    tags("eFormidling")
    summary = "Send en enkelt melding til flere mottakere"
    description = ""
    request {
        body <MeldingBulkDTO> {
            description = "Melding som skal sendes, pluss en liste med personnummer til mottakerne"
            required = true
            example("Enkel melding") {
                value = MeldingBulkDTO(
                    tittel = "God dag!",
                    melding = "Hei! Håper du har en fin dag i dag. Mvh Kartverket",
                    identifikatorer = listOf("12345678910", "0123456789"),
                )
            }
        }
    }
    response {
        code(HttpStatusCode.OK) {
            description =
                "Melding er blitt sendt."
        }
        code(HttpStatusCode.BadRequest) {
            description =
                "Feil ved forespørelen, eller mottakeren mangler digital postkasse."
        }
        code(HttpStatusCode.InternalServerError) {
            description =
                "Intern serverfeil. Dette kan skyldes feil i eFormidling."
        }
    }
}

private val sendEnDoc: RouteConfig.() -> Unit = {
    tags("eFormidling")
    summary = "Send en enkelt melding til en enkelt mottaker"
    description = ""
    request {
        body <MeldingSingleDTO> {
            description = "Melding som skal sendes, pluss personnummer til mottaker"
            required = true
            example("Enkel melding") {
                value = MeldingSingleDTO(
                    tittel = "God dag!",
                    melding = "Hei! Håper du har en fin dag i dag. Mvh Kartverket",
                    identifikator = "12345678910",
                )
            }
        }
    }
    response {
        code(HttpStatusCode.OK) {
            description =
                "Melding er blitt sendt."
        }
        code(HttpStatusCode.BadRequest) {
            description =
                "Feil ved forespørelen, eller mottakeren mangler digital postkasse."
        }
        code(HttpStatusCode.InternalServerError) {
            description =
                "Intern serverfeil. Dette kan skyldes feil i eFormidling."
        }
    }
}

private val resultDoc: RouteConfig.() -> Unit = {
    tags("eFormidling")
    summary = "Hent en liste over sendte meldinger på en viss dato"
    description = ""
    request {
        queryParameter<String>("dato") {
            description = "Dato når meldingene ble sendt"
            required = true
        }
    }
    response {
        code(HttpStatusCode.OK) {
            description =
                "Returnerer en liste med meldinger."
            body<List<ConversationDTO>> {
            }
        }
        code(HttpStatusCode.BadRequest) {
            description =
                "Feil ved forespørelen. Dette kan skyldes ugyldig eller feil format på dato."
        }
        code(HttpStatusCode.InternalServerError) {
            description =
                "Intern serverfeil. Dette kan skyldes feil i eFormidling."
        }
    }
}

private val resultFnrDoc: RouteConfig.() -> Unit = {
    tags("eFormidling")
    summary = "Hent personnummer til alle personer det ble sendt meldinger til på en viss dato"
    description = ""
    request {
        queryParameter<String>("dato") {
            description = "Dato når meldingene ble sendt"
            required = true
        }
    }
    response {
        code(HttpStatusCode.OK) {
            description =
                "Returnerer et sett med personnumre."
            body<Set<Fnr>> {
            }
        }
        code(HttpStatusCode.BadRequest) {
            description =
                "Feil ved forespørelen. Dette kan skyldes ugyldig eller feil format på dato."
        }
        code(HttpStatusCode.InternalServerError) {
            description =
                "Intern serverfeil. Dette kan skyldes feil i eFormidling."
        }
    }
}
