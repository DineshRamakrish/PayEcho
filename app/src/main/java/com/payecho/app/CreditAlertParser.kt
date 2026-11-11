package com.payecho.app

import java.util.Locale

internal object CreditAlertParser {
    private val debitTerms = listOf(
        "you sent",
        "sent to",
        "debited",
        "paid to",
        "payment to",
        "withdrawn",
        "spent",
        "purchase",
        "charged"
    )
    private val creditTerms = listOf(
        "received",
        "credited",
        "credit to",
        "money added",
        "added to your account",
        "paid you",
        "sent you"
    )
    private val amountPattern = Regex(
        """(?:₹|rs\.?\s*|inr\s*)(\d[\d,]*(?:\.\d{1,2})?)|(\d[\d,]*(?:\.\d{1,2})?)\s*(?:rupees|inr)""",
        RegexOption.IGNORE_CASE
    )

    fun receivedAmount(title: String?, message: String?): String? {
        val text = listOfNotNull(title, message).joinToString(" ")
        val normalized = text.lowercase(Locale.ROOT)
        if (debitTerms.any(normalized::contains)) return null
        if (creditTerms.none(normalized::contains)) return null

        val match = amountPattern.find(text) ?: return ""
        return match.groups[1]?.value ?: match.groups[2]?.value.orEmpty()
    }
}
