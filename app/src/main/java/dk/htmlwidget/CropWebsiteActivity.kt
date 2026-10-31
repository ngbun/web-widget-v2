package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

        val prefs =
            getSharedPreferences(
                "settings",
                MODE_PRIVATE
            )

        val address =
            prefs.getString(
                "url_$appWidgetId",
                null
            )?.trim()

        if (
            address.isNullOrBlank() ||
            (
                !address.startsWith("https://") &&
                !address.startsWith("http://")
            )
        ) {

            Toast.makeText(
                this,
                "Der er ikke gemt en gyldig hjemmeside.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val manager =
            AppWidgetManager.getInstance(this)

        val options =
            manager.getAppWidgetOptions(
                appWidgetId
            )

        val density =
            resources.displayMetrics.density

        val widgetWidth =
            (
                options.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                    300
                ) * density
                ).roundToInt()
                .coerceAtLeast(1)

        val widgetHeight =
            (
                options.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                    180
                ) * density
                ).roundToInt()
                .coerceAtLeast(1)

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.WHITE
                )
            }

        val instruction =
            TextView(this).apply {

                text =
                    "Scroll gennem hele hjemmesiden. Placér det ønskede område i den røde ramme og tryk Gem."

                textSize =
                    16f

                setTextColor(
                    Color.BLACK
                )

                setPadding(
                    30,
                    20,
                    30,
                    20
                )
            }

        root.addView(
            instruction
        )

        /*
         * Dette område indeholder både WebView
         * og den røde ramme ovenpå.
         */
        val websiteContainer =
            FrameLayout(this)

        root.addView(
            websiteContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        webView =
            WebView(this).apply {

                setBackgroundColor(
                    Color.WHITE
                )

                settings.apply {

                    javaScriptEnabled =
                        true

                    domStorageEnabled =
                        true

                    loadsImagesAutomatically =
                        true

                    useWideViewPort =
                        true

                    loadWithOverviewMode =
                        false

                    builtInZoomControls =
                        false

                    displayZoomControls =
                        false
                }

                isVerticalScrollBarEnabled =
                    true

                isHorizontalScrollBarEnabled =
                    true

                /*
                 * WebView'en må selv håndtere scrolling.
                 */
                overScrollMode =
                    View.OVER_SCROLL_IF_CONTENT_SCROLLS
            }

        websiteContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        /*
         * Beregn størrelsen på den røde ramme.
         *
         * Worker'en renderer hjemmesiden i 3 ×
         * widgetbredden. Derfor svarer widgetudsnittet
         * til cirka 1/3 af den viste bredde.
         */
        val screenWidth =
            resources.displayMetrics.widthPixels

        val frameWidth =
            (screenWidth / 3f)
                .roundToInt()
                .coerceAtLeast(1)

        val widgetRatio =
            widgetHeight.toFloat() /
                widgetWidth.toFloat()

        val frameHeight =
            (
                frameWidth *
                    widgetRatio
                ).roundToInt()
                .coerceAtLeast(1)

        /*
         * Transparent overlay.
         *
         * Den røde ramme modtager IKKE touch,
         * så fingeren går direkte igennem til WebView.
         */
        val frameOverlay =
            View(this).apply {

                isClickable =
                    false

                isFocusable =
                    false

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.TRANSPARENT
                        )

                        setStroke(
                            6,
                            Color.RED
                        )
                    }
            }

        val frameParams =
            FrameLayout.LayoutParams(
                frameWidth,
                frameHeight
            ).apply {

                gravity =
                    Gravity.CENTER
            }

        websiteContainer.addView(
            frameOverlay,
            frameParams
        )

        val saveButton =
            Button(this).apply {

                text =
                    "Gem dette område"

                setOnClickListener {

                    /*
                     * WebViewens aktuelle scrollposition.
                     */
                    val scrollX =
                        webView.scrollX

                    val scrollY =
                        webView.scrollY

                    /*
                     * Find rammens placering inde
                     * i den synlige WebView.
                     */
                    val frameLeft =
                        (
                            (
                                websiteContainer.width -
                                    frameOverlay.width
                                ) / 2f
                            )

                    val frameTop =
                        (
                            (
                                websiteContainer.height -
                                    frameOverlay.height
                                ) / 2f
                            )

                    /*
                     * WebView-koordinater for øverste
                     * venstre hjørne af rammen.
                     */
                    val selectedScreenX =
                        scrollX +
                            frameLeft

                    val selectedScreenY =
                        scrollY +
                            frameTop

                    /*
                     * Workerens bitmap er 3 × widgetbredden.
                     *
                     * Konverter derfor koordinaterne fra
                     * den synlige WebView til workerens
                     * bitmap-koordinater.
                     */
                    val scaleToWorker =
                        (
                            widgetWidth * 3f
                            ) /
                            websiteContainer.width
                                .coerceAtLeast(1)
                                .toFloat()

                    val cropBitmapX =
                        (
                            selectedScreenX *
                                scaleToWorker
                            ).roundToInt()
                            .coerceAtLeast(0)

                    val cropBitmapY =
                        (
                            selectedScreenY *
                                scaleToWorker
                            ).roundToInt()
                            .coerceAtLeast(0)

                    prefs.edit()
                        .putInt(
                            "cropBitmapX_$appWidgetId",
                            cropBitmapX
                        )
                        .putInt(
                            "cropBitmapY_$appWidgetId",
                            cropBitmapY
                        )
                        .putBoolean(
                            "cropEnabled_$appWidgetId",
                            true
                        )
                        .putInt(
                            "websiteScrollX_$appWidgetId",
                            scrollX
                        )
                        .putInt(
                            "websiteScrollY_$appWidgetId",
                            scrollY
                        )
                        .apply()

                    Toast.makeText(
                        this@CropWebsiteActivity,
                        "Området er gemt",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            }

        root.addView(
            saveButton
        )

        setContentView(
            root
        )

        webView.webViewClient =
            object : WebViewClient() {

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): android.webkit.WebResourceResponse? {

                    val adBlockEnabled =
                        prefs.getBoolean(
                            "adblock",
                            true
                        )

                    if (!adBlockEnabled) {

                        return super.shouldInterceptRequest(
                            view,
                            request
                        )
                    }

                    val host =
                        request?.url
                            ?.host
                            ?.lowercase()
                            ?: return super.shouldInterceptRequest(
                                view,
                                request
                            )

                    val blocked =
                        listOf(
                            "doubleclick.net",
                            "googlesyndication.com",
                            "googleadservices.com",
                            "adservice.google.com",
                            "googletagmanager.com",
                            "google-analytics.com",
                            "scorecardresearch.com",
                            "adnxs.com",
                            "criteo.com",
                            "criteo.net"
                        ).any {

                            host == it ||
                                host.endsWith(
                                    ".$it"
                                )
                        }

                    return if (blocked) {

                        android.webkit.WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            null
                        )

                    } else {

                        super.shouldInterceptRequest(
                            view,
                            request
                        )
                    }
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    /*
                     * Hvis brugeren tidligere har valgt
                     * et område, gå tilbage til omtrent
                     * samme sted på hjemmesiden.
                     */
                    val oldScrollX =
                        prefs.getInt(
                            "websiteScrollX_$appWidgetId",
                            0
                        )

                    val oldScrollY =
                        prefs.getInt(
                            "websiteScrollY_$appWidgetId",
                            0
                        )

                    if (
                        oldScrollX > 0 ||
                        oldScrollY > 0
                    ) {

                        webView.postDelayed({

                            webView.scrollTo(
                                oldScrollX,
                                oldScrollY
                            )

                        }, 1000)
                    }
                }
            }

        webView.loadUrl(
            address
        )
    }

    override fun onDestroy() {

        try {

            webView.stopLoading()
            webView.destroy()

        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}
