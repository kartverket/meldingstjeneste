package meldingstjeneste.model

import kotlinx.serialization.Serializable
import meldingstjeneste.serializers.KZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class AltinnOrderConfirmation(
    val orderId: String,
    val recipientLookup: AltinnRecipientLookup? = null,
)

@Serializable
data class AltinnRecipientLookup(
    val status: AltinnOrderConfirmationStatus,
    val isReserved: List<String>? = null,
    val missingContact: List<String>? = null,
)

@Serializable
data class AltinnSendersReferenceResponse(
    val count: Int,
    val orders: List<AltinnOrderResponse>? = null,
)

@Serializable
data class AltinnOrderRequest(
    @Serializable(with = KZonedDateTimeSerializer::class)
    val requestedSendTime: ZonedDateTime,
    val sendersReference: String,
    val recipients: List<Recipient>,
    val ignoreReservation: Boolean? = null,
    val resourceId: String? = null,
    val conditionEndpoint: String? = null,
    val notificationChannel: NotificationChannel,
    val emailTemplate: AltinnEmailTemplate? = null,
    val smsTemplate: AltinnSmsTemplate? = null,
)

@Serializable
data class AltinnOrderResponse(
    val id: String,
    val sendersReference: String? = null,
    val requestedSendTime: String,
    val creator: String? = null,
    val created: String,
    val notificationChannel: NotificationChannel,
    val recipients: List<Recipient>? = null,
    val emailTemplate: AltinnEmailTemplate? = null,
    val smsTemplate: AltinnSmsTemplate? = null,
)

@Serializable
data class AltinnOrderStatusResponse(
    val id: String,
    val sendersReference: String? = null,
    val requestedSendTime: String,
    val creator: String? = null,
    val created: String,
    val notificationChannel: NotificationChannel,
    val processingStatus: AltinnProcessingStatusResponse,
)

@Serializable
data class AltinnNotificationStatusResponse(
    val orderId: String,
    val sendersReference: String? = null,
    val generated: Int,
    val succeeded: Int,
    val notifications: List<AltinnNotificationResponse>? = null,
)

@Serializable
data class AltinnNotificationResponse(
    val id: String,
    val succeeded: Boolean,
    val recipient: Recipient,
    val sendStatus: AltinnProcessingStatusResponse,
)

@Serializable
data class AltinnProcessingStatusResponse(
    val status: String? = null,
    val description: String? = null,
    val lastUpdate: String,
)

@Serializable
enum class AltinnOrderConfirmationStatus {
    Success,
    PartialSuccess,
    Failed,
}
