package no.kartverket.meldingstjeneste.service

import no.kartverket.meldingstjeneste.clients.ConversationDTO
import no.kartverket.meldingstjeneste.clients.EFormidlingClient
import no.kartverket.meldingstjeneste.clients.EFormidlingMeldingId
import no.kartverket.meldingstjeneste.clients.FysiskPerson
import no.kartverket.meldingstjeneste.clients.Meldingstatus
import no.kartverket.meldingstjeneste.clients.createStandardBusinessDocument

class EFormidlingService {

    private val eFormidlingClient = EFormidlingClient()

    suspend fun opprettMeldingIEFormidling(
        mottaker: FysiskPerson
    ): EFormidlingMeldingId {


        val capabilitiesResponse = eFormidlingClient.getCapabilities(mottaker)

        val capability = capabilitiesResponse.find { it.serviceIdentifier === "DPI" && it.digitalPostAddress?.address != null } ?: run {
            throw MissingDigitalCapabilitiesException("Ingen capabilities")
        }


        val sbd = createStandardBusinessDocument(mottaker, capability)

        val eFormidlingMeldingId = eFormidlingClient.createMessage(sbd)

        return eFormidlingMeldingId

    }

    suspend fun lastOppMeldingsInnhold(
        meldingId: EFormidlingMeldingId,
        tittel: String,
        document: String,
    ) {
        eFormidlingClient.uploadHtmlFile(meldingId, document.toByteArray(), "varsel.html", tittel)
    }


    suspend fun sendMelding(mottaker: FysiskPerson, tittel: String, document: String): Boolean {
        val meldingId = opprettMeldingIEFormidling(mottaker)

        lastOppMeldingsInnhold(meldingId, tittel, document)
        return eFormidlingClient.sendMessage(meldingId)
    }

    suspend fun hentMottakereMedVellykketLevering(datolevert: String): List<ConversationDTO> {
        val res = eFormidlingClient.getOutgoingConversations("Melding om egenregistrering")

        val vellykketLevertMottakere = res.content.filter { conversation ->
            conversation.messageStatuses.any {
                it.status == Meldingstatus.LEVERT &&
                        it.lastUpdate.startsWith(datolevert)
            }
        }

        return vellykketLevertMottakere
    }


}

open class MissingDigitalCapabilitiesException(cause: String): Exception(cause)
