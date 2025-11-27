package no.kartverket.meldingstjeneste.clients

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.SerialName
import java.util.Locale


@Serializable
data class Capabilities(
    val capabilities: List<Capability>,
)

@Serializable
data class Capability(
    val process: String,
    val serviceIdentifier: String,
    val documentTypes: List<DocumentType>,
    val digitalPostAddress: DigitalPostAddress? = null,
    val postAddress: PostAddress? = null,
    val returnAddress: PostAddress? = null,
)

@Serializable
data class DocumentType(
    val type: DocumentTypeStandard,
    val standard: String? = null,
) {
    companion object {
        fun fromString(type: String, standard: String? = null): DocumentType {
            val docTypeStandard = try {
                DocumentTypeStandard.valueOf(type.uppercase(Locale.getDefault()))
            } catch (_: IllegalArgumentException) {
                DocumentTypeStandard.UNSUPPORTED
            }
            return DocumentType(type = docTypeStandard, standard = standard)
        }
    }
}

@Serializable(with = DocumentTypeStandardSerializer::class)
enum class DocumentTypeStandard {
    @SerialName("print")
    PRINT,

    @SerialName("digital")
    DIGITAL,

    @SerialName("arkivmelding")
    ARKIVMELDING,

    @SerialName("unsupported")
    UNSUPPORTED,
}

object DocumentTypeStandardSerializer : KSerializer<DocumentTypeStandard> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DocumentTypeStandard", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DocumentTypeStandard {
        val value = decoder.decodeString()
        return DocumentTypeStandard.entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: DocumentTypeStandard.UNSUPPORTED
    }

    override fun serialize(encoder: Encoder, value: DocumentTypeStandard) {
        val stringValue = when(value) {
            DocumentTypeStandard.PRINT -> "print"
            DocumentTypeStandard.DIGITAL -> "digital"
            DocumentTypeStandard.ARKIVMELDING -> "arkivmelding"
            DocumentTypeStandard.UNSUPPORTED -> "unsupported"
        }
        encoder.encodeString(stringValue)
    }
}



@Serializable
data class DigitalPostAddress(
    val supplier: String,
    val address: String? = null,
)

@Serializable
data class PostAddress(
    val name: String,
    val street: String? = null,
    val postalCode: String? = null,
    val postalArea: String? = null,
    val country: String? = null,
)
