package no.kartverket.meldingstjeneste.service

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.kartverket.meldingstjeneste.env
import no.kartverket.meldingstjeneste.model.AltinnEmailTemplate
import no.kartverket.meldingstjeneste.model.AltinnNotificationResponse
import no.kartverket.meldingstjeneste.model.AltinnNotificationStatusResponse
import no.kartverket.meldingstjeneste.model.AltinnOrderConfirmation
import no.kartverket.meldingstjeneste.model.AltinnOrderRequest
import no.kartverket.meldingstjeneste.model.AltinnOrderResponse
import no.kartverket.meldingstjeneste.model.AltinnOrderStatusResponse
import no.kartverket.meldingstjeneste.model.AltinnSendersReferenceResponse
import no.kartverket.meldingstjeneste.model.AltinnSmsTemplate
import no.kartverket.meldingstjeneste.model.Notification
import no.kartverket.meldingstjeneste.model.NotificationChannel
import no.kartverket.meldingstjeneste.model.NotificationStatus
import no.kartverket.meldingstjeneste.model.Notifications
import no.kartverket.meldingstjeneste.model.NotificationsSummary
import no.kartverket.meldingstjeneste.model.OrderConfirmation
import no.kartverket.meldingstjeneste.model.OrderRequest
import no.kartverket.meldingstjeneste.model.OrderResponse
import no.kartverket.meldingstjeneste.model.OrderStatus
import no.kartverket.meldingstjeneste.model.PaginationOrder
import no.kartverket.meldingstjeneste.model.PaginationOrders
import no.kartverket.meldingstjeneste.model.Recipient
import no.kartverket.meldingstjeneste.model.RecipientLookup
import no.kartverket.meldingstjeneste.model.RecipientMapper
import no.kartverket.meldingstjeneste.plugins.ForbiddenException
import no.kartverket.meldingstjeneste.plugins.UnauthorizedException
import java.time.ZoneOffset
import java.time.ZonedDateTime

class OrderService {
    private val client = HttpClientProvider.client

    private suspend fun postAltinnOrder(body: AltinnOrderRequest): HttpResponse {
        val response = client.post("$NOTIFICATIONS_URL/orders") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        return response
    }

    suspend fun getAltinnOrders(sendersReference: String): HttpResponse =
        client.get("$NOTIFICATIONS_URL/orders?sendersReference=$sendersReference")

    private suspend fun getAltinnOrderInfo(orderId: String): HttpResponse = client.get("$NOTIFICATIONS_URL/orders/$orderId")

    private suspend fun getAltinnOrderStatus(orderId: String): HttpResponse = client.get("$NOTIFICATIONS_URL/orders/$orderId/status")

    private suspend fun cancelAltinnOrder(orderId: String): HttpResponse = client.put("$NOTIFICATIONS_URL/orders/$orderId/cancel")

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
                            fromAddress = "ikke-svar@kartverket.no",
                            contentType = "html",
                            subject = it.subject,
                            body = it.body,
                        )
                    },
                smsTemplate =
                    orderRequest.smsTemplate?.let {
                        AltinnSmsTemplate(
                            senderNumber = "Kartverket",
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

            val altinnOrderInfo = altinnOrderInfoResponse.body<AltinnOrderResponse>()

            // Hent detaljer om alle varslene sendt per kanal, parallelt
            val channelTypes = altinnOrderInfo.notificationChannel.toChannelTypes()
            val altinnNotificationStatusResponses =
                channelTypes
                    .map { channelType ->
                        async { getAltinnNotificationStatus(orderId, channelType) }
                    }.awaitAll()
            checkForException(altinnNotificationStatusResponses)

            val altinnOrderStatus = altinnOrderStatusResponse.body<AltinnOrderStatusResponse>()
            val altinnNotificationStatuses = altinnNotificationStatusResponses.map { it.body<AltinnNotificationStatusResponse>() }

            // Sett sammen informasjonen til en respons
            createOrderResponse(altinnOrderInfo, altinnOrderStatus, altinnNotificationStatuses)
        }

    suspend fun cancelOrder(orderId: String): HttpResponse {
        val altinnOrderCancelResponse = cancelAltinnOrder(orderId)
        if (altinnOrderCancelResponse.status == HttpStatusCode.Conflict) {
            return altinnOrderCancelResponse
        }
        checkForException(listOf(altinnOrderCancelResponse))
        return altinnOrderCancelResponse
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
    ): OrderConfirmation {
        val recipients = orderRequest.toRecipients()
        val sendTime = orderRequest.requestedSendTime?.withZoneSameInstant(ZoneOffset.UTC) ?: ZonedDateTime.now(ZoneOffset.UTC)
        return altinnOrderConfirmation.toOrderConfirmation(recipients, sendTime)
    }

    private fun AltinnOrderConfirmation.toOrderConfirmation(
        recipients: List<Recipient>,
        requestedSendTime: ZonedDateTime,
    ): OrderConfirmation {
        val statusLink =  "orders/$orderId"

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
        val BASE_URL: String = env["ALTINN_BASE_URL"]
        val NOTIFICATIONS_URL = "$BASE_URL/notifications/api/v1"
    }
}
