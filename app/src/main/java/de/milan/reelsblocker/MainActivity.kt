package de.milan.reelsblocker

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad * 2, pad, pad)
        }

        val title = TextView(this).apply {
            text = "Reels Blocker"
            textSize = 26f
        }

        val info = TextView(this).apply {
            text = "Sobald du in Instagram den Reels-Bereich öffnest, springt die App " +
                "automatisch zurück zum Feed.\n\n" +
                "So aktivierst du sie:\n" +
                "1. Unten auf \"Bedienungshilfen öffnen\" tippen\n" +
                "2. \"Reels Blocker\" auswählen und einschalten\n\n" +
                "Falls Android die Option ausgraut (bei APKs von außerhalb des Play Store): " +
                "Einstellungen > Apps > Reels Blocker > Menü oben rechts (drei Punkte) > " +
                "\"Eingeschränkte Einstellungen zulassen\"."
            textSize = 16f
            setPadding(0, pad, 0, pad)
        }

        status = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, pad)
        }

        val button = Button(this).apply {
            text = "Bedienungshilfen öffnen"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(status)
        layout.addView(button)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (isServiceEnabled()) {
            "Status: aktiv ✓"
        } else {
            "Status: noch nicht aktiviert"
        }
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, ReelsBlockerService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }
}
