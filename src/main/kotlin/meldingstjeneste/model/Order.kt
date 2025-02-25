package meldingstjeneste.model

import kotlinx.serialization.Serializable
import meldingstjeneste.serializers.KZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class OrderConfirmation(
    val id: String,
    val orderStatus: OrderStatus?,
    val recipientLookup: RecipientLookup?,
    val requestedSendTime: String?,
    val statusLink: String?,
)

@Serializable
data class RecipientLookup(
    val recipientLookupStatus: AltinnOrderConfirmationStatus,
    val validRecipients: List<String>? = null,
    val reservedRecipients: List<String>? = null,
    val missingContactRecipients: List<String>? = null,
)

@Serializable
data class OrderResponse(
    val id: String,
    val orderStatus: OrderStatus,
    val notifications: Notifications,
    val sendersReference: String? = null,
    val requestedSendTime: String,
    val created: String,
    val notificationChannel: NotificationChannel,
    val emailTemplate: AltinnEmailTemplate? = null,
    val smsTemplate: AltinnSmsTemplate? = null,
)

@Serializable
data class Notifications(
    val notificationsList: List<Notification>? = null,
    val summary: NotificationsSummary,
)

@Serializable
data class Notification(
    var status: String,
    val description: String? = null,
    val recipient: Recipient,
    val lastUpdate: String,
)

@Serializable
data class NotificationsSummary(
    val count: Int,
    val delivered: Int,
    val failed: Int,
    val notIdentified: Int,
)

@Serializable
data class PaginationOrders(
    val orders: List<PaginationOrder>,
    val isLastPage: Boolean,
    val numberOfOrders: Int,
    val nextPageNumber: Int,
)

@Serializable
data class PaginationOrder(
    val id: String,
    val orderStatus: OrderStatus,
    val requestedSendTime: String,
    val notificationsSummary: NotificationsSummary,
)

@Serializable
data class OrderRequest(
    val nationalIdentityNumbers: List<String>,
    val notificationChannel: NotificationChannel,
    val smsTemplate: SmsTemplate? = null,
    val emailTemplate: EmailTemplate? = null,
    @Serializable(with = KZonedDateTimeSerializer::class)
    val requestedSendTime: ZonedDateTime? = null,
    val sendersReference: String,
)

@Serializable
data class SmsTemplate(
    val body: String
)

@Serializable
data class EmailTemplate(
    val body: String,
    val subject: String
)

@Serializable
data class Recipient(
    val emailAddress: String? = null,
    val mobileNumber: String? = null,
    val organizationNumber: String? = null,
    val nationalIdentityNumber: String? = null,
    val isReserved: Boolean = false,
)

@Serializable
data class AltinnEmailTemplate(
    val fromAddress: String? = null,
    val subject: String? = null,
    val body: String? = null,
    val contentType: String,
)

@Serializable
data class AltinnSmsTemplate(
    val senderNumber: String? = null,
    val body: String? = null,
)

@Serializable
enum class NotificationChannel {
    Email,
    Sms,
    EmailPreferred,
    SmsPreferred,
}

@Serializable
enum class OrderStatus {
    Scheduled,
    Processing,
    Completed,
    Failed,
    Cancelled,
}

@Serializable
enum class NotificationStatus {
    Processing,
    Delivered,
    Failed,
    NotIdentified,
}
