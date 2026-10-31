
package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class CropWebsiteActivity : Activity() {

    private var appWidgetId =
        AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

        val website =
            intent.getStringExtra("website")
                ?: return finish()

        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        layout.addView(
            TextView(this).apply {
                text =
                    "Flyt og zoom hjemmesiden, så det område du vil vise er på skærmen."
                textSize = 16f
                setPadding(30, 20, 30, 20)
            }
        )

        webView =
            WebView(this).apply {

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true

                webViewClient =
                    WebViewClient()

                loadUrl(website)
            }

        layout.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        layout.addView(
            Button(this).apply {
                text = "Gem dette område"

                setOnClickListener {

    val prefs =
        getSharedPreferences(
            "settings",
            MODE_PRIVATE
        )

    webView.evaluateJavascript(
        """
        (function() {
            return JSON.stringify({
                x: window.scrollX,
                y: window.scrollY,
                width: document.documentElement.scrollWidth,
                height: document.documentElement.scrollHeight
            });
        })();
        """.trimIndent()
    ) { result ->

        try {

            val clean =
                result
                    .removePrefix("\"")
                    .removeSuffix("\"")
                    .replace("\\\"", "\"")

            val json =
                org.json.JSONObject(clean)

            val pageWidth =
                json.getDouble("width")
                    .coerceAtLeast(1.0)

            val pageHeight =
                json.getDouble("height")
                    .coerceAtLeast(1.0)

            val cropXRatio =
                (json.getDouble("x") / pageWidth)
                    .toFloat()

            val cropYRatio =
                (json.getDouble("y") / pageHeight)
                    .toFloat()

            prefs.edit()
                .putFloat(
                    "cropXRatio_$appWidgetId",
                    cropXRatio
                )
                .putFloat(
                    "cropYRatio_$appWidgetId",
                    cropYRatio
                )
                .putFloat(
                    "cropScale_$appWidgetId",
                    webView.scale
                )
                .putBoolean(
                    "cropEnabled_$appWidgetId",
                    true
                )
                .apply()

            finish()

        } catch (e: Exception) {

            android.widget.Toast.makeText(
                this@CropWebsiteActivity,
                "Kunne ikke gemme området",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
                }
            }
        )

        setContentView(layout)
    }
}
