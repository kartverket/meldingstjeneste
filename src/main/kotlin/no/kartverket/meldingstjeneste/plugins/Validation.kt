package no.kartverket.meldingstjeneste.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import no.kartverket.meldingstjeneste.model.NotificationChannel
import no.kartverket.meldingstjeneste.model.OrderRequest
import java.time.ZonedDateTime

val nationalIdRegex = Regex("^\\d{11}$")

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<OrderRequest> { order ->
            with(order) {
                when {
                    nationalIdentityNumbers.isEmpty() -> {
                        ValidationResult.Invalid("The list of national identity numbers is empty.")
                    }

                    !nationalIdentityNumbers.all { it.matches(nationalIdRegex) } -> {
                        ValidationResult.Invalid("All national identity numbers must be 11 digits long.")
                    }

                    requestedSendTime != null && !requestedSendTime.isAfter(ZonedDateTime.now().minusMinutes(1)) -> {
                        ValidationResult.Invalid("The requested send time should be in the future. Leave blank to send immediately.")
                    }

                    // Validering basert på distribusjonskanal
                    notificationChannel == NotificationChannel.Sms && smsTemplate == null -> {
                        ValidationResult.Invalid("smsTemplate is required for choosen distribution channel.")
                    }

                    notificationChannel == NotificationChannel.Email && emailTemplate == null -> {
                        ValidationResult.Invalid("emailTemplate is required for choosen distribution channel.")
                    }

                    (notificationChannel == NotificationChannel.SmsPreferred || notificationChannel == NotificationChannel.EmailPreferred) &&
                        (smsTemplate == null || emailTemplate == null) -> {
                        ValidationResult.Invalid(
                            "Both smsTemplate and emailTemplate are required for preferred distribution channels.",
                        )
                    }

                    // Validering for SMS body-lengde. Tekstlengde over 268 vil telle som tre SMS-er og transaksjoner.
                    (smsTemplate?.body?.length ?: 0) > 268 -> {
                        ValidationResult.Invalid("The SMS body should not exceed 268 characters.")
                    }

                    else -> {
                        ValidationResult.Valid
                    }
                }
            }
        }
    }
}
