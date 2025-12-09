package no.kartverket.meldingstjeneste.routes

import io.ktor.server.request.receive
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import no.kartverket.meldingstjeneste.service.EFormidlingService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.coroutines.launch
import no.kartverket.meldingstjeneste.clients.FysiskPerson
import no.kartverket.meldingstjeneste.logger
import io.ktor.server.routing.get


fun Route.eFormidlingroutes(eFormidlingService: EFormidlingService) {
        post("/eFormidling/send") {
            val (tittel, melding, identifikatorer) = call.receive<MeldingDTO>()

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


        get("/eFormidling/result") {
            val params = call.request.queryParameters
            val datolevert = params["dato"]!!
           val vellykkedeutsendinger =  eFormidlingService.hentMottakereMedVellykketLevering(datolevert)
            call.respond(vellykkedeutsendinger)
        }
    get("/eFormidling/result/fnr") {
        val params = call.request.queryParameters
        val datolevert = params["dato"]!!
        val vellykkedeutsendinger =  eFormidlingService.hentMottakereMedVellykketLevering(datolevert).map { conversationDTO -> conversationDTO.receiver }.toSet()

        call.respond(vellykkedeutsendinger)

    }

}

@Serializable
data class MeldingDTO(
    val tittel: String,
    val melding: String,
    val identifikatorer: List<String>,
)
