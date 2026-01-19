package no.kartverket.meldingstjeneste.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import no.kartverket.meldingstjeneste.clients.EFormidlingServerException
import no.kartverket.meldingstjeneste.clients.FysiskPerson
import no.kartverket.meldingstjeneste.logger
import no.kartverket.meldingstjeneste.service.EFormidlingService
import no.kartverket.meldingstjeneste.service.MissingCapabilitiesException


fun Route.eFormidlingroutes(eFormidlingService: EFormidlingService) {
    post("/eFormidling/send/bulk") {
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

    post("/eFormidling/send/single") {
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
                is EFormidlingServerException -> {
                    logger.error("eFormidling sendte en serverfeil. Sending av melding kan sannsynligvis prøves igjen senere.", e)
                    call.respond(HttpStatusCode.InternalServerError, APIErrorResponse(APIErrorType.SERVER_FEIL, "eFormidling sendte en serverfeil. Melding kan sannsynligvis prøves igjen senere") )
                }
                is MissingCapabilitiesException -> {
                    logger.info("Mottaker har ikke digital capability, sender ikke melding til eFormidling")
                    call.respond(HttpStatusCode.BadRequest, APIErrorResponse(APIErrorType.MOTTAKER_MANGLER_DIGITAL_CAPABILITY,"Bruker mangler digitial capability: ${e.message}"))
                }

                else -> {
                    logger.error("Uventet feil ved sending til eFormidling", e)
                    call.respond(HttpStatusCode.InternalServerError, APIErrorResponse(APIErrorType.SERVER_FEIL, "Uventet feil ved sending til eFormidling"))}
            }
        }

    }


    get("/eFormidling/result") {
        val params = call.request.queryParameters
        val datolevert = params["dato"]!!
        val vellykkedeutsendinger = eFormidlingService.hentMottakereMedVellykketLevering(datolevert)
        call.respond(vellykkedeutsendinger)
    }
    get("/eFormidling/result/fnr") {
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
enum class APIErrorType(val type: String) {
    SERVER_FEIL("GENERELL_FEIL"),
    MOTTAKER_MANGLER_DIGITAL_CAPABILITY("MOTTAKER_MANGLER_DIGITAL_CAPABILITY"),
}

