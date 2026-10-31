package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

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

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            finish()
            return
        }

        val website =
            intent.getStringExtra("website")
                ?: run {
                    finish()
                    return
                }

        val manager =
            AppWidgetManager.getInstance(this)

        val options =
            manager.getAppWidgetOptions(appWidgetId)

        val density =
            resources.displayMetrics.density

        val widgetWidth = (
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                300
            ) * density
        ).roundToInt().coerceAtLeast(1)

        val widgetHeight = (
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                180
            ) * density
        ).roundToInt().coerceAtLeast(1)

        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        layout.addView(
            TextView(this).apply {
                text =
                    "Flyt hjemmesiden til præcis det område, der skal vises i widgetten."
                textSize = 16f
                setPadding(30, 20, 30, 20)
            }
        )

        webView =
            WebView(this).apply {

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = false
                settings.offscreenPreRaster = true

                webViewClient =
                    WebViewClient()

                loadUrl(website)
            }

        layout.addView(
            webView,
            LinearLayout.LayoutParams(
                widgetWidth,
                widgetHeight
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
                                y: window.scrollY
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

                            val scrollX =
                                json.getDouble("x")
                                    .roundToInt()

                            val scrollY =
                                json.getDouble("y")
                                    .roundToInt()

                            prefs.edit()
                                .putInt(
                                    "cropScrollX_$appWidgetId",
                                    scrollX
                                )
                                .putInt(
                                    "cropScrollY_$appWidgetId",
                                    scrollY
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
