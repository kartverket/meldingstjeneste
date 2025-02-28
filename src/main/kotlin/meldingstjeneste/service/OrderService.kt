package meldingstjeneste.service

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import meldingstjeneste.env
import meldingstjeneste.model.*
import meldingstjeneste.plugins.ForbiddenException
import meldingstjeneste.plugins.UnauthorizedException
import java.time.ZoneOffset
import java.time.ZonedDateTime

class OrderService {
    private val client = HttpClientProvider.client

    private suspend fun postAltinnOrder(body: AltinnOrderRequest): HttpResponse =
        client.post("$NOTIFICATIONS_URL/orders") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    suspend fun getAltinnOrders(sendersReference: String): HttpResponse =
        client.get("$NOTIFICATIONS_URL/orders?sendersReference=$sendersReference")

    private suspend fun getAltinnOrderInfo(orderId: String): HttpResponse = client.get("$NOTIFICATIONS_URL/orders/$orderId")

    private suspend fun getAltinnOrderStatus(orderId: String): HttpResponse = client.get("$NOTIFICATIONS_URL/orders/$orderId/status")

    private suspend fun getAltinnNotificationStatus(
        orderId: String,
        channelType: String,
    ): HttpResponse = client.get("$NOTIFICATIONS_URL/orders/$orderId/notifications/$channelType")

    suspend fun sendOrder(orderRequest: OrderRequest): HttpResponse {
        val recipients = orderRequest.toRecipients()

        val body =
            AltinnOrderRequest(
                recipients = recipients,
                emailTemplate =
                    orderRequest.emailTemplate?.let {
                        AltinnEmailTemplate(
                            fromAddress = "Statens Kartverk",
                            contentType = "Plain",
                            subject = it.subject,
                            body = it.body,
                        )
                    },
                smsTemplate =
                    orderRequest.smsTemplate?.let {
                        AltinnSmsTemplate(
                            senderNumber = "Statens Kartverk",
                            body = it.body,
                        )
                    },
                notificationChannel = orderRequest.notificationChannel,
                requestedSendTime =
                    orderRequest.requestedSendTime?.withZoneSameInstant(ZoneOffset.UTC)
                        ?: ZonedDateTime.now(ZoneOffset.UTC),
                sendersReference = orderRequest.sendersReference,
            )

        return postAltinnOrder(body)
    }

    suspend fun getOrderStatus(orderId: String): OrderResponse =
        coroutineScope {
            // Hent og prosesser informasjon om ordren, parallelt
            val altinnOrderInfoDeferred = async { getAltinnOrderInfo(orderId) }
            val altinnOrderStatusDeferred = async { getAltinnOrderStatus(orderId) }

            val altinnOrderInfoResponse = altinnOrderInfoDeferred.await()
            val altinnOrderStatusResponse = altinnOrderStatusDeferred.await()
            checkForException(listOf(altinnOrderInfoResponse, altinnOrderStatusResponse))

            val altinnOrderInfo: AltinnOrderResponse = altinnOrderInfoResponse.body()

            // Hent detaljer om alle varslene sendt per kanal, parallelt
            val channelTypes = altinnOrderInfo.notificationChannel.toChannelTypes()
            val altinnNotificationStatusResponses =
                channelTypes
                    .map { channelType ->
                        async { getAltinnNotificationStatus(orderId, channelType) }
                    }.awaitAll()
            checkForException(altinnNotificationStatusResponses)

            val altinnOrderStatus: AltinnOrderStatusResponse = altinnOrderStatusResponse.body()
            val altinnNotificationStatuses = altinnNotificationStatusResponses.map { it.body<AltinnNotificationStatusResponse>() }

            // Sett sammen informasjonen til en respons
            createOrderResponse(altinnOrderInfo, altinnOrderStatus, altinnNotificationStatuses)
        }

    private fun createOrderResponse(
        orderInfo: AltinnOrderResponse,
        altinnOrderStatus: AltinnOrderStatusResponse,
        altinnNotificationStatus: List<AltinnNotificationStatusResponse>,
    ): OrderResponse {
        val notifications = altinnNotificationStatus.flatMap { it.notifications ?: emptyList() }
        val (notificationsStatus, notificationsSummary) = notifications.toNotificationStatus()
        val orderStatus: OrderStatus = altinnOrderStatus.toOrderStatus(notificationsStatus)

        return OrderResponse(
            id = orderInfo.id,
            orderStatus = orderStatus,
            notifications =
                Notifications(
                    notificationsList = notificationsStatus,
                    summary =
                        NotificationsSummary(
                            count = orderInfo.recipients?.size ?: 0,
                            delivered = notificationsSummary.delivered,
                            failed = notificationsSummary.failed,
                            notIdentified = notificationsSummary.notIdentified,
                        ),
                ),
            sendersReference = orderInfo.sendersReference,
            requestedSendTime = orderInfo.requestedSendTime,
            created = orderInfo.created,
            notificationChannel = orderInfo.notificationChannel,
            emailTemplate = orderInfo.emailTemplate,
            smsTemplate = orderInfo.smsTemplate,
        )
    }

    fun createOrderConfirmation(
        altinnOrderConfirmation: AltinnOrderConfirmation,
        orderRequest: OrderRequest,
        host: String,
    ): OrderConfirmation {
        val recipients = orderRequest.toRecipients()
        val sendTime = orderRequest.requestedSendTime?.withZoneSameInstant(ZoneOffset.UTC) ?: ZonedDateTime.now(ZoneOffset.UTC)
        return altinnOrderConfirmation.toOrderConfirmation(recipients, sendTime, host)
    }

    private fun AltinnOrderConfirmation.toOrderConfirmation(
        recipients: List<Recipient>,
        requestedSendTime: ZonedDateTime,
        host: String,
    ): OrderConfirmation {
        val ingress = env["INGRESS"]
        val statusLink =  "$ingress/orders/$orderId"

        val orderStatus: OrderStatus =
            if (checkIfDateIsLaterThanNow(requestedSendTime.toString())) {
                OrderStatus.Scheduled
            } else {
                OrderStatus.Processing
            }

        val recipientLookupResponse =
            recipientLookup?.let { lookup ->
                val reservedRecipients = lookup.isReserved
                val missingContactRecipients = lookup.missingContact
                val validRecipients =
                    recipients
                        .filter { recipient ->
                            missingContactRecipients != null &&
                                reservedRecipients != null &&
                                !reservedRecipients.contains(recipient.nationalIdentityNumber.toString()) &&
                                !missingContactRecipients.contains(recipient.nationalIdentityNumber.toString())
                        }.map { it.nationalIdentityNumber ?: it.emailAddress ?: it.mobileNumber ?: it.organizationNumber ?: "" }

                RecipientLookup(
                    recipientLookupStatus = lookup.status,
                    validRecipients = validRecipients,
                    reservedRecipients = reservedRecipients,
                    missingContactRecipients = missingContactRecipients,
                )
            }

        return OrderConfirmation(
            id = orderId,
            orderStatus = orderStatus,
            recipientLookup = recipientLookupResponse,
            requestedSendTime = requestedSendTime.toString(),
            statusLink = statusLink,
        )
    }

    private fun OrderRequest.toRecipients() =
        nationalIdentityNumbers.map { nationalIdentityNumber ->
            RecipientMapper.mapToRecipient(nationalIdentityNumber)
        }

    private fun List<AltinnNotificationResponse>.toNotificationStatus(): Pair<List<Notification>, NotificationsSummary> {
        var failed = 0
        var notIdentified = 0
        var delivered = 0
        val notifications =
            this.map {
                val notification = it.toNotification()
                when (notification.status) {
                    NotificationStatus.Failed.toString() -> failed++
                    NotificationStatus.NotIdentified.toString() -> notIdentified++
                    NotificationStatus.Delivered.toString() -> delivered++
                }
                notification
            }
        return Pair(
            notifications,
            NotificationsSummary(
                count = notifications.size,
                delivered = delivered,
                failed = failed,
                notIdentified = notIdentified,
            ),
        )
    }

    private fun AltinnNotificationResponse.toNotification(): Notification {
        val newStatus =
            when (sendStatus.status) {
                "New", "Sending", "Accepted", "Succeeded" -> NotificationStatus.Processing.toString()
                "Delivered" -> NotificationStatus.Delivered.toString()
                "Failed_RecipientNotIdentified" -> NotificationStatus.NotIdentified.toString()
                else -> NotificationStatus.Failed.toString()
            }

        return Notification(
            status = newStatus,
            description = sendStatus.description,
            recipient = recipient,
            lastUpdate = sendStatus.lastUpdate,
        )
    }

    private fun AltinnOrderStatusResponse.toOrderStatus(notificationStatus: List<Notification>?): OrderStatus =
        when {
            processingStatus.status == "Cancelled" -> OrderStatus.Cancelled
            checkIfDateIsLaterThanNow(requestedSendTime) -> OrderStatus.Scheduled
            notificationStatus?.isEmpty() == true -> OrderStatus.Processing
            notificationStatus?.all {
                it.status == NotificationStatus.Failed.toString() || it.status == NotificationStatus.NotIdentified.toString()
            } == true -> OrderStatus.Failed
            notificationStatus?.all {
                it.status in
                    listOf(
                        NotificationStatus.Delivered.toString(),
                        NotificationStatus.Failed.toString(),
                        NotificationStatus.NotIdentified.toString(),
                    )
            } == true -> OrderStatus.Completed
            else -> OrderStatus.Processing
        }

    private fun NotificationChannel.toChannelTypes(): List<String> =
        when (this) {
            NotificationChannel.Email -> listOf("email")
            NotificationChannel.Sms -> listOf("sms")
            else -> listOf("sms", "email")
        }

    private suspend fun checkForException(responses: List<HttpResponse>) {
        responses.forEach { response ->
            when (response.status) {
                HttpStatusCode.BadRequest -> throw BadRequestException(response.bodyAsText())
                HttpStatusCode.Unauthorized -> throw UnauthorizedException(response.bodyAsText())
                HttpStatusCode.Forbidden -> throw ForbiddenException(response.bodyAsText())
                HttpStatusCode.NotFound -> throw NotFoundException(message = response.bodyAsText())
                HttpStatusCode.InternalServerError -> throw Exception(response.bodyAsText())
                else ->
                    if (!response.status.isSuccess()) {
                        throw Exception("Unhandled error: ${response.status.value} - ${response.bodyAsText()}")
                    }
            }
        }
    }

    suspend fun paginateOrders(
        sendersReference: String,
        orderType: String? = null,
        index: Int = 0,
    ): PaginationOrders =
        coroutineScope {
            // Hent ut alle ordre for avsender
            val altinnOrdersResponse = getAltinnOrders(sendersReference)
            checkForException(listOf(altinnOrdersResponse))

            val allOrders: AltinnSendersReferenceResponse = altinnOrdersResponse.body()
            val ordersSorted: List<AltinnOrderResponse>? = sortOrdersByRequestedSendTime(allOrders.orders, orderType)
            if (ordersSorted != null) {
                // Paginer ordrene
                val secondIndex = minOf(index + 5, ordersSorted.size)
                val ordersSliced: List<AltinnOrderResponse> = ordersSorted.subList(index, secondIndex)
                val isLastPage: Boolean = ordersSorted.lastIndex < index + 5
                // Hent ut status for hver ordre, parallelt
                val fullOrderStatusList: List<PaginationOrder> =
                    ordersSliced.map { async { getPaginatedOrder(it, it.recipients?.size ?: 0) } }.awaitAll()

                PaginationOrders(
                    orders = fullOrderStatusList,
                    isLastPage = isLastPage,
                    numberOfOrders = ordersSorted.size,
                    nextPageNumber = secondIndex,
                )
            } else {
                PaginationOrders(
                    orders = emptyList(),
                    isLastPage = true,
                    numberOfOrders = 0,
                    nextPageNumber = 0,
                )
            }
        }

    private suspend fun getPaginatedOrder(
        orderFromSendersReference: AltinnOrderResponse,
        numberOfNotifications: Int,
    ): PaginationOrder =
        coroutineScope {
            val altinnOrderStatusDeferred = async { getAltinnOrderStatus(orderFromSendersReference.id) }

            // Hent detaljer om alle varslene sendt per kanal, parallelt
            val channelTypes = orderFromSendersReference.notificationChannel.toChannelTypes()
            val notificationStatusResponse =
                channelTypes
                    .map { channelType ->
                        async { getAltinnNotificationStatus(orderFromSendersReference.id, channelType) }
                    }.awaitAll()

            val orderStatusAltinnResponse = altinnOrderStatusDeferred.await()
            checkForException(notificationStatusResponse.plus(orderStatusAltinnResponse))

            val altinnNotificationStatuses = notificationStatusResponse.map { it.body<AltinnNotificationStatusResponse>() }
            val altinnOrderStatus: AltinnOrderStatusResponse = orderStatusAltinnResponse.body()

            createPaginationOrder(orderFromSendersReference, altinnOrderStatus, altinnNotificationStatuses, numberOfNotifications)
        }

    private fun createPaginationOrder(
        orderFromSendersReference: AltinnOrderResponse,
        altinnOrderStatus: AltinnOrderStatusResponse,
        altinnNotificationStatus: List<AltinnNotificationStatusResponse>,
        numberOfNotifications: Int,
    ): PaginationOrder {
        val notifications = altinnNotificationStatus.flatMap { it.notifications ?: emptyList() }
        val (notificationStatus, notificationSummary) = notifications.toNotificationStatus()
        val orderStatus: OrderStatus = altinnOrderStatus.toOrderStatus(notificationStatus)

        return PaginationOrder(
            id = orderFromSendersReference.id,
            orderStatus = orderStatus,
            requestedSendTime = orderFromSendersReference.requestedSendTime,
            notificationsSummary =
                NotificationsSummary(
                    count = numberOfNotifications,
                    delivered = notificationSummary.delivered,
                    failed = notificationSummary.failed,
                    notIdentified = notificationSummary.notIdentified,
                ),
        )
    }

    fun sortOrdersByRequestedSendTime(
        orders: List<AltinnOrderResponse>?,
        type: String? = null,
    ): List<AltinnOrderResponse>? =
        when (type) {
            "active" ->
                orders
                    ?.filter { !checkIfDateIsLaterThanNow(it.requestedSendTime) }
                    ?.sortedByDescending { order ->
                        ZonedDateTime.parse(order.requestedSendTime)
                    }
            "planned" ->
                orders
                    ?.filter { checkIfDateIsLaterThanNow(it.requestedSendTime) }
                    ?.sortedBy { order ->
                        ZonedDateTime.parse(order.requestedSendTime)
                    }
            else ->
                orders
                    ?.sortedByDescending { order ->
                        ZonedDateTime.parse(order.requestedSendTime)
                    }
        }

    private fun checkIfDateIsLaterThanNow(date: String): Boolean = ZonedDateTime.parse(date).isAfter(ZonedDateTime.now().plusMinutes(5))

    companion object {
        val BASE_URL = env["ALTINN_BASE_URL"]
        val NOTIFICATIONS_BASE_URL = "/notifications/api/v1"
        val NOTIFICATIONS_URL = "$BASE_URL$NOTIFICATIONS_BASE_URL"
    }
}
