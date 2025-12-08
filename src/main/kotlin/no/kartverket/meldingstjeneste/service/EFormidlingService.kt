package no.kartverket.meldingstjeneste.service

import no.kartverket.meldingstjeneste.clients.DocumentTypeStandard
import no.kartverket.meldingstjeneste.clients.EFormidlingClient
import no.kartverket.meldingstjeneste.clients.EFormidlingMeldingId
import no.kartverket.meldingstjeneste.clients.FysiskPerson
import no.kartverket.meldingstjeneste.clients.createStandardBusinessDocument
import no.kartverket.meldingstjeneste.logger

class EFormidlingService {

    private val eFormidlingClient = EFormidlingClient()

    suspend fun opprettMeldingIEFormidling(
        mottaker: FysiskPerson
    ): EFormidlingMeldingId? {


        val capabilitiesResponse = eFormidlingClient.getCapabilities(mottaker)

        val capability = capabilitiesResponse.firstOrNull() ?: run {
            throw IllegalArgumentException("Ingen capabilities")
        }

        if (capability.documentTypes.first().type == DocumentTypeStandard.PRINT) {
            logger.info("Mottaker har ikke digital capability, sender ikke melding til eFormidling")
            return null
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

        if (meldingId != null) {
            lastOppMeldingsInnhold(meldingId, tittel, document)
            return eFormidlingClient.sendMessage(meldingId)

        }

        return false
    }

}
