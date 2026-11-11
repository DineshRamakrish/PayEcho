package com.payecho.app

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.ArrayDeque
import java.util.LinkedHashMap
import java.util.Locale

class PaymentNotificationListener : NotificationListenerService() {
    private var textToSpeech: TextToSpeech? = null
    private var speechReady = false
    private val pendingAlerts = ArrayDeque<String>()
    private val recentAlerts = LinkedHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        initializeSpeech()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in GOOGLE_PAY_PACKAGES || sbn.isOngoing) return

        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (!isAlertEnabled()) return

        val extras = notification.extras ?: Bundle.EMPTY
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val message = notificationText(extras)
        val amount = CreditAlertParser.receivedAmount(title, message) ?: return
        if (isDuplicate(sbn.key)) return

        val spokenAlert = if (amount.isBlank()) {
            "A payment was credited."
        } else {
            "Money received: $amount."
        }
        speak(spokenAlert)
    }

    override fun onDestroy() {
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }

    private fun initializeSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            speechReady = status == TextToSpeech.SUCCESS
            if (speechReady) {
                textToSpeech?.language = Locale.getDefault()
                while (pendingAlerts.isNotEmpty()) speak(pendingAlerts.removeFirst())
            }
        }
    }

    private fun speak(message: String) {
        if (speechReady) {
            textToSpeech?.speak(
                message,
                TextToSpeech.QUEUE_ADD,
                null,
                "payecho-credit-${SystemClock.elapsedRealtime()}"
            )
        } else if (pendingAlerts.size < MAX_PENDING_ALERTS) {
            pendingAlerts.addLast(message)
        }
    }

    private fun isAlertEnabled() =
        getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getBoolean(MainActivity.KEY_ALERTS_ENABLED, true)

    private fun notificationText(extras: Bundle): String =
        buildList {
            extras.getCharSequence(Notification.EXTRA_TEXT)?.let { add(it.toString()) }
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { add(it.toString()) }
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.forEach { add(it.toString()) }
        }.distinct().joinToString(" ")

    private fun isDuplicate(key: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val lastAlert = recentAlerts[key]
        if (lastAlert != null && now - lastAlert < DUPLICATE_WINDOW_MS) return true

        recentAlerts[key] = now
        while (recentAlerts.size > MAX_RECENT_ALERTS) {
            recentAlerts.remove(recentAlerts.keys.first())
        }
        return false
    }

    companion object {
        private val GOOGLE_PAY_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.google.android.apps.walletnfcrel"
        )
        private const val DUPLICATE_WINDOW_MS = 5_000L
        private const val MAX_PENDING_ALERTS = 5
        private const val MAX_RECENT_ALERTS = 100
    }
}
