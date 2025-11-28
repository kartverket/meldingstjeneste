package no.kartverket.meldingstjeneste.service

import no.kartverket.meldingstjeneste.clients.DocumentTypeStandard
import no.kartverket.meldingstjeneste.clients.EFormidlingClient
import no.kartverket.meldingstjeneste.clients.EFormidlingMeldingId
import no.kartverket.meldingstjeneste.clients.FysiskPerson
import no.kartverket.meldingstjeneste.clients.createStandardBusinessDocument
import org.slf4j.LoggerFactory


class eFormidlingService {

    private val eFormidlingClient = EFormidlingClient()
    val logger = LoggerFactory.getLogger(javaClass)


    suspend fun opprettMeldingIEFormidling(
        mottaker: FysiskPerson
    ): EFormidlingMeldingId {


        val capabilitiesResponse = eFormidlingClient.getCapabilities(mottaker)

        val capability = capabilitiesResponse.firstOrNull() ?: run {
            throw IllegalArgumentException("Ingen capabilities")
        }

        if (capability.documentTypes.first().type == DocumentTypeStandard.PRINT) {
            throw IllegalArgumentException("Mottaker støtter kun print")
        }
        val sbd = createStandardBusinessDocument(mottaker, capability)

        val eFormidlingMeldingId = eFormidlingClient.createMessage(sbd)

        return eFormidlingMeldingId

    }

    suspend fun lastOppMeldingsInnhold(
        meldingId: EFormidlingMeldingId,
    ) {
        val html = this::class.java.classLoader.getResource("varsel.html")?.readBytes()
            ?: throw IllegalStateException("Fant ikke varsel.html i resources")

        eFormidlingClient.uploadHtmlFile(meldingId, html, "varsel.html", "Registrer opplysninger i Eiendomsregisteret")
    }


    suspend fun sendMelding(mottaker: FysiskPerson) {

        try {
            val meldingId = opprettMeldingIEFormidling(mottaker)
            lastOppMeldingsInnhold(meldingId)

            eFormidlingClient.sendMessage(meldingId)
        } catch (e: Exception) {
            throw e

        }
    }

}
