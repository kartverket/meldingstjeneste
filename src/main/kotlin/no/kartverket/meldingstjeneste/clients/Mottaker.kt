package no.kartverket.meldingstjeneste.clients

import kotlinx.serialization.Serializable

@Serializable
data class FysiskPerson(
    val identifikator: String,
    val prosessType: String = "urn:no:difi:profile:digitalpost:info:ver1.0"
)
