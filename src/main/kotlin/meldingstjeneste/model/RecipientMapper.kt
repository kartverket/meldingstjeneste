package meldingstjeneste.model

import meldingstjeneste.env

object RecipientMapper {
    // List of nationalIdentityNumbers mapped to specific email addresses.
    // Used to send test emails during development without altering contact information in the Tenor database.

    private val nationalIdentityNumbers: Map<String, String> =
        mapOf(
            "00000000000" to env["TEST_EMAIL_ADDRESS"],
            )

    fun mapToRecipient(nationalIdentityNumber: String): Recipient {
        val email = nationalIdentityNumbers[nationalIdentityNumber]
        return if (email != null) {
            Recipient(emailAddress = email)
        } else {
            Recipient(nationalIdentityNumber = nationalIdentityNumber)
        }
    }
}
