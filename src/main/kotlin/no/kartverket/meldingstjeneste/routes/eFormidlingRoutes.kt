package no.kartverket.meldingstjeneste.routes

import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import no.kartverket.meldingstjeneste.service.eFormidlingService
import io.ktor.server.response.respond
import no.kartverket.meldingstjeneste.clients.FysiskPerson
import no.kartverket.meldingstjeneste.logger


fun Application.eFormidlingroutes(eFormidlingService: eFormidlingService) {
    routing {
        post("eFormidling/send") {
            val (identifikatorer) = call.receive<MeldingDTO>()

            val mottakere = identifikatorer.map { id ->
                FysiskPerson(
                    identifikator = id
                )
            }

            logger.info("Sender melding til eFormidling for ${mottakere.size} mottakere")

            mottakere.forEach { mottaker ->
                try {
                    eFormidlingService.sendMelding(mottaker)
                }

                catch (e: Exception) {
                    logger.error("Kunne ikke sende melding", e)
                    throw e
                }

            }

            logger.info("Ferdig med å sende meldinger til eFormidling")
            // TODO - lage coroutine async for å besvare endepunkt raskere
            call.respond("Melding sendt til eFormidling")
        }
    }
}

@Serializable
data class MeldingDTO(
    val identifikatorer: List<String>,
)
