package no.kartverket.meldingstjeneste.routes

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.kartverket.meldingstjeneste.auth.getUserId
import no.kartverket.meldingstjeneste.internal.Metrics
import no.kartverket.meldingstjeneste.logger
import no.kartverket.meldingstjeneste.model.AltinnOrderConfirmation
import no.kartverket.meldingstjeneste.model.AltinnOrderStatusResponse
import no.kartverket.meldingstjeneste.model.AltinnSendersReferenceResponse
import no.kartverket.meldingstjeneste.model.EmailTemplate
import no.kartverket.meldingstjeneste.model.NotificationChannel
import no.kartverket.meldingstjeneste.model.OrderConfirmation
import no.kartverket.meldingstjeneste.model.OrderRequest
import no.kartverket.meldingstjeneste.model.OrderResponse
import no.kartverket.meldingstjeneste.model.PaginationOrders
import no.kartverket.meldingstjeneste.model.SmsTemplate
import no.kartverket.meldingstjeneste.service.OrderService
import java.time.ZonedDateTime

fun Route.orderRoutes(orderService: OrderService) {
    post("/orders", ordersDoc) {
        val request = call.receive<OrderRequest>()
        logger.info(
            "Received order request from ${request.sendersReference} with notificationChannel ${request.notificationChannel}. Sent by ${call.getUserId()}",
        )

        val requestWithUniqueIdentityNumbers =
            request.copy(nationalIdentityNumbers = request.nationalIdentityNumbers.distinct())
        val response = orderService.sendOrder(requestWithUniqueIdentityNumbers)

        if (response.status.isSuccess()) {
            Metrics.antallOrdreBestilt.increment()
            val body = response.body<AltinnOrderConfirmation>()
            val orderConfirmation = orderService.createOrderConfirmation(body, request)
            logger.info("Successfully sent order request ${orderConfirmation.id} to Altinn")
            call.respond(response.status, orderConfirmation)
        } else {
            logger.info("Order request to Altinn failed with status code: ${response.status.value}")
            call.respond(response)
        }
    }

    get("/orders/{id}", statusDoc) {
        val orderId = call.parameters["id"].toString()
        val status = orderService.getOrderStatus(orderId)
        logger.info("Responding to request for order with ID $orderId: status ${status.orderStatus}")

        call.respond(HttpStatusCode.OK, status)
    }

    put("/orders/{id}/cancel", cancelDoc) {
        val orderId = call.parameters["id"].toString()

        val response = orderService.cancelOrder(orderId)
        if (response.status.isSuccess()) {
            val body = response.body<AltinnOrderStatusResponse>()
            logger.info("Canceling order with ID $orderId")
            call.respond(HttpStatusCode.OK, body)
        } else if (response.status == HttpStatusCode.Conflict) {
            logger.info("Unable to cancel order with ID $orderId")
            call.respond(response.status, "Ordre kan ikke lengre avbrytes. (ID $orderId)")
        }
        call.respond(response.status)
    }

    get("/orders", paginationDoc) {
        val type = call.request.queryParameters["type"]
        val sendersReference =
            call.request.queryParameters["sendersReference"]
                ?: throw IllegalArgumentException("'sendersReference' is required")
        val index =
            call.request.queryParameters["index"]!!.toIntOrNull()
                ?: throw IllegalArgumentException("'index' is required and should be an integer")
        logger.info("Received request for all orders from $sendersReference")
        val response = orderService.paginateOrders(sendersReference = sendersReference, orderType = type, index = index)
        call.respond(HttpStatusCode.OK, response)
    }

    get("/orders/ids/{sendersReference}", orderIdsDoc) {
        val sendersReference =
            call.parameters["sendersReference"] ?: throw IllegalArgumentException("'sendersReference' is required")
        val response = orderService.getAltinnOrders(sendersReference = sendersReference)

        if (response.status.isSuccess()) {
            val body = response.body<AltinnSendersReferenceResponse>()
            val orderInfoSorted = orderService.sortOrdersByRequestedSendTime(body.orders)
            val orderIds = orderInfoSorted?.map { it.id }
            call.respond(response.status, orderIds ?: emptyList())
        } else {
            call.respond(response)
        }
    }
}

private val ordersDoc: RouteConfig.() -> Unit = {
    tags("Varslinger")
    summary = "Send varsel til en liste med fødselsnummer"
    description =
        "Bestill utsending av varsler til én eller flere mottakere basert på fødselsnummer. \n\n" +
        "Endepunktet benytter Altinns varslingstjeneste til utsending. Denne tjenesten henter ut den digitale kontaktinformasjonen til " +
        "mottakerne i [Kontakt- og reservasjonsregisteret](https://eid.difi.no/nb/kontakt-og-reservasjonsregisteret). " +
        "Pass derfor på at bruksområdet ditt går inn under Udirs rettninslinjer for lovpålagt eller valgrfri bruk av KRR."
    request {
        body<OrderRequest> {
            description =
                "Data som kreves for å opprette en ordre: \n" +
                "- _nationalIdentityNumbers_: Mottakerne som skal varsles. Fødselsnummer må være på 11 siffer.\n" +
                "- `smsTemplate`: Tekst for varsel på SMS. Det er ikke tillatt med klikkbare elementer som lenker og e-post.\n" +
                "- `emailTemplate`: Emne og tekst for varsel sendt på e-post. Det er ikke tillatt med klikkbare elementer \n" +
                "- `notificationChannel`: Hvilken kanal varselet skal sendes på." +
                "Velger du eksplisitt e-post eller SMS, vil vi kun forsøke å nå mottakere via den valgte kanalen." +
                "His mottaker mangler kontaktinformasjon for denne kanalen i KRR, vil ikke varselet bli sendt." +
                "Velger du en foretrukket kanal, vil varselet sendes på den foretrukne kanalen hvis kontaktinformasjon er tilgjengelig. " +
                "Hvis ikke, forsøker vi å sende via den andre kanalen. \n" +
                "- `requestedSendTime`: Ønsket tidspunkt for utsending. Default er umiddelbar utsending. \n" +
                "- `sendersReference`: Unik referanse oppgitt av avsenderen for sporing og historikk. " +
                "Denne trengs for å kunne hente ut alle ordre som er sendt fra samme avsender." +
                "Dersom du ønsker historikk bør du derfor bruke samme referanse for alle ordre som går ut fra samme tjeneste"
            required = true
            example("Eksempel-ordre") {
                value =
                    OrderRequest(
                        nationalIdentityNumbers = listOf("12345678901", "10987654321"),
                        emailTemplate =
                            EmailTemplate(
                                body = "Hei, dette er en testmelding fra Kartverket. Logg inn på Kartverket med din bruker for å lese den.",
                                subject = "Nytt varsel fra Kartverket",
                            ),
                        smsTemplate = SmsTemplate("Hei, dette er en testmelding fra Kartverket."),
                        notificationChannel = NotificationChannel.SmsPreferred,
                        requestedSendTime = ZonedDateTime.now(),
                        sendersReference = "Kartverket_teamnavn_applikasjonsnavn",
                    )
            }
        }
    }
    response {
        code(HttpStatusCode.Accepted) {
            description = "Bestillingen er mottatt og behandles av Altinn."
            "- `id`: En unik ID for bestillingen som kan brukes til å spore statusen til ordren og alle dens varsler.\n" +
                "- `recipientLookup`: Informasjon om statusen for initielt mottakeroppslag i KRR. " +
                "Returnere en liste over mottakere som er gyldige, reserverte eller mangler kontaktinformasjon.\n" +
                "- `requestedSendTime`: Tidspunktet for utsending av varsel, i ISO 8601-format.\n" +
                "- `orderStatus`: Status for bestillingen, som ved opprettelse kan være 'Scheduled' eller 'Processing'.\n" +
                "- `statusLink`: En lenke for å sjekke statusen til ordren."
            body<OrderConfirmation> {
                required = true
            }
        }
        code(HttpStatusCode.BadRequest) {
            description =
                "Feil ved forespørelen. Dette kan skyldes ugyldige ID-numre eller feil format på forespørselen."
        }
        code(HttpStatusCode.InternalServerError) {
            description =
                "Intern serverfeil. Klarte ikke å opprette ordren. Dette kan skyldes feil i Altinns varslingstjeneste."
        }
    }
}

private val statusDoc: RouteConfig.() -> Unit = {
    tags("Varslinger")
    summary = "Hent status for en ordre"
    description =
        "Endepunkt for å hente ut detaljert informasjon om en ordre og status på alle dens varsler. Her kan du følge med på hvordan det går med varslene du bestilte."
    request {
        queryParameter<String>("id") {
            description = "Ordre-id. Denne mottar du som respons ved opprettelse av en ordre."
            required = true
        }
    }
    response {
        code(HttpStatusCode.OK) {
            description =
                "Informasjon om ordren og status for utsending. " +
                "Inkluderer både en overdnet oversikt over ordren og dens status, samt status for hvert enkelt varsel/mottaker."
            body<OrderResponse> {
            }
        }
        code(HttpStatusCode.NotFound) {
            description =
                "Ordren ble ikke funnet."
        }
        code(HttpStatusCode.BadRequest) {
            description =
                "Feil ved forespørelen. Dette kan skyldes ugyldige ID-numre eller feil format på forespørselen."
        }
        code(HttpStatusCode.InternalServerError) {
            description =
                "Intern serverfeil. Dette kan skyldes feil i Altinns varslingstjeneste."
        }
    }
}

private val cancelDoc: RouteConfig.() -> Unit = {
    tags("Varslinger")
    summary = "Avbryt en bestilt ordre"
    description =
        "Endepunkt for å avbryte en bestilt ordre. Hvis orderen har kommet for langt i sin prosess, vil den ikke kunne avbrytes."
    request {
        queryParameter<String>("id") {
            description = "Ordre-id. Denne mottar du som respons ved opprettelse av en ordre."
            required = true
        }
    }
    response {
        code(HttpStatusCode.OK) {
            description =
                "Informasjon om ordren og status for utsending. " +
                "Inkluderer både en overdnet oversikt over ordren og dens status, samt status for hvert enkelt varsel/mottaker."
            body<AltinnOrderStatusResponse> {}
        }
        code(HttpStatusCode.Conflict) {
            description =
                "Orderen kan ikke avsluttes fordi den er kommet for langt i prosessen."
        }
        code(HttpStatusCode.NotFound) {
            description =
                "Ordren ble ikke funnet."
        }
        code(HttpStatusCode.BadRequest) {
            description =
                "Feil ved forespørelen. Dette kan skyldes ugyldige ID-numre eller feil format på forespørselen."
        }
        code(HttpStatusCode.InternalServerError) {
            description =
                "Intern serverfeil. Dette kan skyldes feil i Altinns varslingstjeneste."
        }
    }
}

private val paginationDoc: RouteConfig.() -> Unit = {
    tags("Varslinger")
    summary = "Hent en paginert liste med ordre og deres status"
    description =
        "Paginert endepunkt for å hente ut alle ordre som er sendt fra en spesifikk avsender. " +
        "Endepunktet returnerer også overordnet status for hver ordre " +
        "slik at du enkelt får oversikt over hvordan det har gått med de ulike."
    request {
        queryParameter<String>("type") {
            description =
                "Hent ut enten aktive eller planlagte ordre. Gyldige verdier er 'active' eller 'planned'. " +
                "Hvis du ikke spesifiserer type vil vi returnere begge typer sortert på dato lengst fram i tid til eldst."
            required = false
        }
        queryParameter<String>("sendersReference") {
            description = "Avsenderen du vil hente ut ordrene for."
            required = true
        }
        queryParameter<String>("index") {
            description = "Hvilken index du vil hente ut ordre fra. Brukes for paginering."
            required = true
        }
    }
    response {
        code(HttpStatusCode.OK) {
            description =
                "Returnerer et objekt bestående av nødvendig informasjon for paginering, samt en liste med ordrene."
            body<PaginationOrders> {
            }
        }
        code(HttpStatusCode.BadRequest) {
            description =
                "Feil ved forespørelen. Dette kan skyldes ugyldige sendersReference eller feil format på forespørselen."
        }
        code(HttpStatusCode.InternalServerError) {
            description =
                "Intern serverfeil. Dette kan skyldes feil i Altinns varslingstjeneste."
        }
    }
}

private val orderIdsDoc: RouteConfig.() -> Unit = {
    description =
        "Endepunkt for å hente ut ordreid-er på en gitt sendersReference"
    tags("Varslinger")
    request {
        queryParameter<String>("sendersReference") {
            description = "Senders reference"
            required = true
        }
    }
    response {
        default {
            description = "Alle ordreid-er på en gitt sendersReference."
            body<List<String>> {
            }
        }
        code(HttpStatusCode.InternalServerError) {
            description =
                "Intern serverfeil. Dette kan skyldes feil i Altinns varslingstjeneste."
        }
    }
}
