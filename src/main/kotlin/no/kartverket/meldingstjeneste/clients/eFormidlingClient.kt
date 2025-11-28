package no.kartverket.meldingstjeneste.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

import no.kartverket.meldingstjeneste.env
import org.slf4j.LoggerFactory

class EFormidlingClient {
    private val logger = LoggerFactory.getLogger(EFormidlingClient::class.java)
    private val eFormidlingURL = "${env["E_FORMIDLING_URL"]}/api"

    private val client =
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    explicitNulls = false
                    encodeDefaults = true
                })
            }
        }

    suspend fun getCapabilities(mottaker: FysiskPerson): List<Capability> {
        val res = client.get("$eFormidlingURL/capabilities/${mottaker.identifikator}?process=${mottaker.prosessType}")
        if (!res.status.isSuccess()) {
            val error = res.body<EFormidlingErrorResponse>()
            logger.error("Error while getting capabilities", error.error)
            throw IllegalArgumentException("Kall til capabilities feilet")
        }

        val responseBody = res.body<Capabilities>()

        return responseBody.capabilities.map { cap ->
            Capability(
                process = cap.process,
                serviceIdentifier = cap.serviceIdentifier,
                documentTypes = cap.documentTypes.map { doc ->
                    DocumentType.fromString(doc.type.name, doc.standard)
                },
                digitalPostAddress = cap.digitalPostAddress,
                postAddress = cap.postAddress,
                returnAddress = cap.returnAddress
            )
        }
    }

    suspend fun createMessage(sbd: StandardBusinessDocument): EFormidlingMeldingId {
        val res = client.post("$eFormidlingURL/messages/out") {
            contentType(ContentType.Application.Json)
            setBody(sbd)
        }
        if (!res.status.isSuccess()) {
            val error = res.body<EFormidlingErrorResponse>()
            when (val exceptionType = error.exception.split('.').last()) {
                "MissingAddressInformationException" -> throw IllegalArgumentException("Feil mot eFormidling")
                "ServiceNotEnabledException" -> throw IllegalArgumentException("Feil mot eFormidling")
                else -> throw RuntimeException("${error.message} ($exceptionType)")
            }
        }
        val response = res.body<StandardBusinessDocument>()
        return response.standardBusinessDocumentHeader.documentIdentification.instanceIdentifier!!
    }


    suspend fun uploadHtmlFile(
        eFormidlingMeldingId: EFormidlingMeldingId,
        html: ByteArray,
        filename: String,
        attachmentName: String = filename
    ) {

        val res = client.put("$eFormidlingURL/messages/out/$eFormidlingMeldingId") {
            headers {
                append(HttpHeaders.ContentDisposition, "attachment; name=\"$attachmentName\"; filename=\"$filename\"")
                append(HttpHeaders.ContentLength, html.size.toString())
                append(HttpHeaders.ContentType, "application/octet-stream")
            }
            setBody(html)
        }

        if (!res.status.isSuccess()) {
            val msg = "Opplasting av fil feilet – ${res.status} – ${res.bodyAsText()}"
            logger.error(msg)
            throw RuntimeException(msg)
        }
    }



    suspend fun sendMessage(eFormidlingMeldingId: EFormidlingMeldingId) {
        val res = client.post("$eFormidlingURL/messages/out/$eFormidlingMeldingId")
        if (!res.status.isSuccess()) {
            val msg = "Sending av melding feilet – ${res.status} – ${res.bodyAsText()}"
            logger.error(msg)
            throw RuntimeException(msg)
        }
    }





}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class EFormidlingErrorResponse(
    val timestamp: String,
    val status: Int,
    val error: String,
    val exception: String,
    val message: String,
    val path: String,
    val description: String? = null,
)
