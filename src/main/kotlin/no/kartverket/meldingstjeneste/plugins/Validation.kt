package no.kartverket.meldingstjeneste.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import no.kartverket.meldingstjeneste.model.NotificationChannel
import no.kartverket.meldingstjeneste.model.OrderRequest
import java.time.ZonedDateTime

val nationalIdRegex = Regex("^\\d{11}$")

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<OrderRequest> { order ->
            with(order) {
                when {
                    nationalIdentityNumbers.isEmpty() ->
                        ValidationResult.Invalid("The list of national identity numbers is empty.")
                    !nationalIdentityNumbers.all { it.matches(nationalIdRegex) } ->
                        ValidationResult.Invalid("All national identity numbers must be 11 digits long.")
                    requestedSendTime != null && !requestedSendTime.isAfter(ZonedDateTime.now().minusMinutes(1)) ->
                        ValidationResult.Invalid("The requested send time should be in the future. Leave blank to send immediately.")

                    // Validering basert på distribusjonskanal
                    notificationChannel == NotificationChannel.Sms && smsTemplate == null ->
                        ValidationResult.Invalid("smsTemplate is required for choosen distribution channel.")
                    notificationChannel == NotificationChannel.Email && emailTemplate == null ->
                        ValidationResult.Invalid("emailTemplate is required for choosen distribution channel.")
                    (notificationChannel == NotificationChannel.SmsPreferred || notificationChannel == NotificationChannel.EmailPreferred)
                            && (smsTemplate == null || emailTemplate == null) ->
                        ValidationResult.Invalid("Both smsTemplate and emailTemplate are required for preferred distribution channels.")

                    // Validering for SMS body-lengde. Tekstlengde over 157 vil telle som to transaksjoner med dobbel kostnad.
                    (smsTemplate?.body?.length ?: 0) > 157 ->
                        ValidationResult.Invalid("The SMS body should not exceed 157 characters.")

                    else -> ValidationResult.Valid
                }
            }
        }
    }
}
