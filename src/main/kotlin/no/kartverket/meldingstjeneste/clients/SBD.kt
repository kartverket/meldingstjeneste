package no.kartverket.meldingstjeneste.clients

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.jsonObject
import java.util.UUID


const val SENDER_REF_ID = "b7b1f7a7-6f4a-4d0c-9f6e-6b8f2c3e9a41"


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

    return when (documentType.type) {
        DocumentTypeStandard.PRINT -> StandardBusinessDocument.PrintSbd(
            standardBusinessDocumentHeader = header,
            print = PrintPayload(
                hoveddokument = "varsel.html",
                mottaker = Postadresse(
                    navn = capability.postAddress!!.name,
                    adresselinje1 = capability.postAddress.street,
                    postnummer = capability.postAddress.postalCode,
                    poststed = capability.postAddress.postalArea,
                    land = capability.postAddress.country
                ),
            )
        )

        DocumentTypeStandard.DIGITAL -> StandardBusinessDocument.DigitalSbd(
            standardBusinessDocumentHeader = header,
            digital = DigitalPostPayload(
                sikkerhetsnivaa = 3,
                hoveddokument = "varsel.html",
                tittel = "Melding om egenregistrering",
                digitalPostInfo = DigitalPostInfo(
                    virkningsdato = "2025-01-01",
                    aapningskvittering = true
                )
            )
        )

        DocumentTypeStandard.ARKIVMELDING -> StandardBusinessDocument.ArkivmeldingSbd(
            standardBusinessDocumentHeader = header,
            arkivmelding = ArkivmeldingPayload(
                dpv = Dpv(
                    varselType = "VarselDPVMedRevarsel",
                    varselTransportType = "EpostOgSMS"
                )
            )
        )

        else -> error("Ukjent dokumenttype '${documentType.type}' for mottaker")
    }
}

/**
 * Fra Digdirs standard for business dokumenter:
 * https://docs.digdir.no/docs/eFormidling/Utvikling/Dokumenttyper/standard_sbd
 */
@Serializable(with = SbdSerializer::class)
sealed class StandardBusinessDocument {
    abstract val standardBusinessDocumentHeader: StandardBusinessDocumentHeader

    @Serializable
    data class DigitalSbd(
        override val standardBusinessDocumentHeader: StandardBusinessDocumentHeader,
        val digital: DigitalPostPayload,
    ) : StandardBusinessDocument()

    @Serializable
    data class ArkivmeldingSbd(
        override val standardBusinessDocumentHeader: StandardBusinessDocumentHeader,
        val arkivmelding: ArkivmeldingPayload,
    ) : StandardBusinessDocument()

    @Serializable
    data class PrintSbd(
        override val standardBusinessDocumentHeader: StandardBusinessDocumentHeader,
        val print: PrintPayload,
    ) : StandardBusinessDocument()
}

object SbdSerializer : JsonContentPolymorphicSerializer<StandardBusinessDocument>(StandardBusinessDocument::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<StandardBusinessDocument> {
        return when {
            "digital" in element.jsonObject -> StandardBusinessDocument.DigitalSbd.serializer()
            "print" in element.jsonObject -> StandardBusinessDocument.PrintSbd.serializer()
            "arkivmelding" in element.jsonObject -> StandardBusinessDocument.ArkivmeldingSbd.serializer()
            else -> error("Unknown type: $element")
        }
    }
}

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
data class ArkivmeldingPayload(
    val dpv: Dpv,
)

@Serializable
data class Dpv(
    val varselType: String = "VarselDPVMedRevarsel",
    val varselTransportType: String = "EpostOgSMS",
)

/**
 * Print payload
 */
@Serializable
@SerialName("printPayload")
@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys // Fanger opp printinstruksjoner m.m.
data class PrintPayload(
    val hoveddokument: String,
    val mottaker: Postadresse? = null,
    val utskriftsfarge: String = "FARGE",
    val posttype: String = "B_OEKONOMI",
    val retur: Retur? = null,
)

@Serializable
data class Retur(
    val returhaandtering: String = "DIREKTE_RETUR",
    val mottaker: Postadresse? = null,
)

@Serializable
data class Postadresse(
    val navn: String,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val adresselinje4: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val land: String? = null,
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
