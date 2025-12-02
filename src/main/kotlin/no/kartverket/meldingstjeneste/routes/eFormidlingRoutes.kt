package no.kartverket.meldingstjeneste.routes

import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import no.kartverket.meldingstjeneste.service.eFormidlingService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.coroutines.launch
import no.kartverket.meldingstjeneste.clients.FysiskPerson
import no.kartverket.meldingstjeneste.logger


fun Route.eFormidlingroutes(eFormidlingService: eFormidlingService) {
        post("/eFormidling/send") {
            val (tittel, melding, identifikatorer) = call.receive<MeldingDTO>()

            val mottakere = identifikatorer.map { id ->
                FysiskPerson(
                    identifikator = id
                )
            }

            logger.info("Sender melding til eFormidling for ${mottakere.size} mottakere")

            call.application.launch {
                mottakere.forEach { mottaker ->
                    try {
                        eFormidlingService.sendMelding(mottaker, tittel, melding)
                    } catch (e: Exception) {
                        logger.error("Kunne ikke sende melding", e)
                        throw e
                    }

                } }


            logger.info("Startet med å sende meldinger til eFormidling")
            call.respond("Sender meldinger")
        }
}

@Serializable
data class MeldingDTO(
    val tittel: String,
    val melding: String,
    val identifikatorer: List<String>,
)
