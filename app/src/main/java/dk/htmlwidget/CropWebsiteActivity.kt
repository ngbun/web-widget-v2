
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

    val contentWidth =
    webView.computeHorizontalScrollRange()
        .coerceAtLeast(1)

val contentHeight =
    webView.computeVerticalScrollRange()
        .coerceAtLeast(1)

val cropXRatio =
    webView.scrollX.toFloat() /
    contentWidth.toFloat()

val cropYRatio =
    webView.scrollY.toFloat() /
    contentHeight.toFloat()

prefs.edit()
    .putFloat(
        "cropXRatio_$appWidgetId",
        cropXRatio
    )
    .putFloat(
        "cropYRatio_$appWidgetId",
        cropYRatio
    )
        .putInt(
            "cropScrollY_$appWidgetId",
            webView.scrollY
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
}
            }
        )

        setContentView(layout)
    }
}
