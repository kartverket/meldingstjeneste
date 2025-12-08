package no.kartverket.meldingstjeneste.clients

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import java.util.UUID
import no.kartverket.meldingstjeneste.SENDER_REF_ID


fun createStandardBusinessDocument(
    mottaker: FysiskPerson,
    capability: Capability
): StandardBusinessDocument {



    val documentType = capability.documentTypes.firstOrNull()
        ?: error("Ingen dokumenttyper tilgjengelig for mottaker")

    val header = StandardBusinessDocumentHeader(
        receiver = listOf(
            Ident(
                identifier = Identifier(
                    value = mottaker.identifikator
                )
            )
        ),
        documentIdentification = DocumentIdentification(
            type = documentType.type,
            standard = documentType.standard,
            instanceIdentifier = UUID.randomUUID().toString(),

            ),
        businessScope = Scope(
            listOf(
                Conversation(identifier = mottaker.prosessType),
                SenderRef(
                    instanceIdentifier = SENDER_REF_ID,
                )
            )
        )
    )


       return StandardBusinessDocument(
            standardBusinessDocumentHeader = header,
            digital = DigitalPostPayload(
                sikkerhetsnivaa = 3,
                hoveddokument = "varsel.html",
                tittel = "Melding om egenregistrering", // Endrer du denne tittelen vil det får konsekvenser for eFormidlingClient.getOutgoingConversations()
                digitalPostInfo = DigitalPostInfo(
                    virkningsdato = "2025-01-01",
                    aapningskvittering = true
                )
            )
        )
}

/**
 * Fra Digdirs standard for business dokumenter:
 * https://docs.digdir.no/docs/eFormidling/Utvikling/Dokumenttyper/standard_sbd
 */
@Serializable()
data class StandardBusinessDocument (
    val standardBusinessDocumentHeader: StandardBusinessDocumentHeader,
    val digital: DigitalPostPayload,
    )

@Serializable
data class StandardBusinessDocumentHeader(
    val headerVersion: String = "1.0",
    val sender: List<Ident>? = null,
    val receiver: List<Ident>,
    val documentIdentification: DocumentIdentification,
    val businessScope: Scope,
)

@Serializable
data class DigitalPostPayload(
    val sikkerhetsnivaa: Int,
    val hoveddokument: String,
    val tittel: String,
    val spraak: String = "NO",
    val metadataFiler: Map<String, String>? = null,
    val digitalPostInfo: DigitalPostInfo,
)

@Serializable
data class DigitalPostInfo(
    val virkningsdato: String,
    val aapningskvittering: Boolean,
)

@Serializable
data class Ident(
    val identifier: Identifier,
)

@Serializable
data class Identifier(
    val authority: String = "iso6523-actorid-upis",
    val value: String,
)

@Serializable
data class DocumentIdentification(
    val standard: String? = null,
    val typeVersion: String = "1.0",
    val instanceIdentifier: InstanceIdentifier? = null,
    val type: DocumentTypeStandard,
    val creationDateAndTime: String? = null,
)

@Serializable
data class Scope(
    val scope: List<ScopeType>,
)

@Serializable
sealed class ScopeType

@Serializable
@SerialName("ConversationId")
data class Conversation(
    val identifier: String,
    val instanceIdentifier: ConversationId? = null,
    val scopeInformation: List<ScopeInformation>? = null,
) : ScopeType()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
@SerialName("SenderRef")
data class SenderRef(
    val instanceIdentifier: SenderRefId,
) : ScopeType()

@Serializable
data class ScopeInformation(
    val expectedResponseDateTime: String,
)

typealias InstanceIdentifier = String
typealias EFormidlingMeldingId = InstanceIdentifier
typealias ConversationId = InstanceIdentifier
typealias SenderRefId = InstanceIdentifier
