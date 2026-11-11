package com.payecho.app

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var accessStatus: TextView
    private lateinit var testButton: Button
    private var testSpeech: TextToSpeech? = null
    private var testSpeechReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding = (24 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        content.addView(TextView(this).apply {
            text = "PayEcho"
            textSize = 32f
        })
        content.addView(TextView(this).apply {
            text = "Hear a voice alert when Google Pay reports money received."
            textSize = 17f
            setPadding(0, padding / 3, 0, padding)
        })

        accessStatus = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, padding / 2)
        }
        content.addView(accessStatus)

        content.addView(Button(this).apply {
            text = "Grant notification access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }, buttonLayoutParams())

        content.addView(Switch(this).apply {
            text = "Speak Google Pay credit alerts"
            isChecked = preferences().getBoolean(KEY_ALERTS_ENABLED, true)
            setOnCheckedChangeListener { _, checked ->
                preferences().edit().putBoolean(KEY_ALERTS_ENABLED, checked).apply()
            }
        }, buttonLayoutParams())

        testButton = Button(this).apply {
            text = "Test voice alert"
            isEnabled = false
            setOnClickListener {
                if (testSpeechReady) {
                    testSpeech?.speak(
                        "PayEcho is ready.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "payecho-test"
                    )
                }
            }
        }
        content.addView(testButton, buttonLayoutParams())

        content.addView(TextView(this).apply {
            text = "PayEcho reads Google Pay notifications on this device only. It does not "
                .plus("send or save notification contents. Android notification access is ")
                .plus("required; you can revoke it in Settings at any time.")
            textSize = 14f
            setPadding(0, padding, 0, 0)
        })

        setContentView(content)
        initializeTestSpeech()
    }

    override fun onDestroy() {
        testSpeech?.shutdown()
        testSpeech = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        refreshAccessStatus()
    }

    private fun refreshAccessStatus() {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )?.split(':')
            ?.mapNotNull { ComponentName.unflattenFromString(it) }
            ?.any { it.packageName == packageName } == true

        accessStatus.text = if (enabled) {
            "Notification access: enabled"
        } else {
            "Notification access: not enabled. Grant access to start voice alerts."
        }
    }

    private fun buttonLayoutParams() =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = (12 * resources.displayMetrics.density).toInt()
        }

    private fun preferences() =
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun initializeTestSpeech() {
        testSpeech = TextToSpeech(this) { status ->
            testSpeechReady = status == TextToSpeech.SUCCESS
            testButton.isEnabled = testSpeechReady
            if (testSpeechReady) testSpeech?.language = Locale.getDefault()
        }
    }

    companion object {
        const val PREFERENCES_NAME = "payecho_settings"
        const val KEY_ALERTS_ENABLED = "credit_alerts_enabled"
    }
}
