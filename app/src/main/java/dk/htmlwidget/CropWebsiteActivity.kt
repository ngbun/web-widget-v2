package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
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

    private var frameX = 0f
    private var frameY = 0f

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

        /*
         * Hent den konkrete widgets størrelse.
         */
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

        /*
         * Widgettens rigtige bredde/højde-forhold.
         */
        val widgetRatio =
            widgetHeight.toFloat() /
                widgetWidth.toFloat()

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.WHITE
                )
            }

        root.addView(
            TextView(this).apply {

                text =
                    "Scroll på hjemmesiden. Træk den røde ramme til det område, der skal vises i widgetten."

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
        )

        /*
         * WebView og rammen ligger oven på hinanden
         * i denne container.
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
         * Rammen skal passe til widgettens form.
         *
         * Vi bruger cirka 70 % af skærmbredden som
         * udgangspunkt. Højden beregnes derefter
         * ud fra widgettens faktiske proportioner.
         */
        val screenWidth =
            resources.displayMetrics.widthPixels

        var frameWidth =
            (screenWidth * 0.70f)
                .roundToInt()
                .coerceAtLeast(1)

        var frameHeight =
            (
                frameWidth *
                    widgetRatio
                ).roundToInt()
                .coerceAtLeast(1)

        /*
         * Hvis widgetten er meget høj, må rammen
         * ikke blive højere end det synlige område.
         *
         * websiteContainer er endnu ikke målt her,
         * så den endelige kontrol foretages også
         * efter layout.
         */
        val frameOverlay =
            View(this).apply {

                isClickable =
                    true

                isFocusable =
                    true

                elevation =
                    20f

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.argb(
                                20,
                                255,
                                0,
                                0
                            )
                        )

                        setStroke(
                            7,
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
                    Gravity.TOP or
                        Gravity.START
            }

        websiteContainer.addView(
            frameOverlay,
            frameParams
        )

        /*
         * Når containeren kender sin rigtige størrelse,
         * sørger vi for at rammen passer indenfor.
         */
        websiteContainer.post {

            val availableWidth =
                websiteContainer.width
                    .coerceAtLeast(1)

            val availableHeight =
                websiteContainer.height
                    .coerceAtLeast(1)

            frameWidth =
                (availableWidth * 0.70f)
                    .roundToInt()
                    .coerceAtLeast(1)

            frameHeight =
                (
                    frameWidth *
                        widgetRatio
                    ).roundToInt()
                    .coerceAtLeast(1)

            /*
             * Hvis højden bliver for stor,
             * beregner vi i stedet ud fra højden.
             */
            val maxFrameHeight =
                (availableHeight * 0.70f)
                    .roundToInt()
                    .coerceAtLeast(1)

            if (frameHeight > maxFrameHeight) {

                frameHeight =
                    maxFrameHeight

                frameWidth =
                    (
                        frameHeight /
                            widgetRatio
                        ).roundToInt()
                        .coerceAtLeast(1)
            }

            frameOverlay.layoutParams =
                FrameLayout.LayoutParams(
                    frameWidth,
                    frameHeight
                )

            /*
             * Start rammen midt på skærmen.
             */
            frameX =
                (
                    availableWidth -
                        frameWidth
                    ) / 2f

            frameY =
                (
                    availableHeight -
                        frameHeight
                    ) / 2f

            frameOverlay.x =
                frameX

            frameOverlay.y =
                frameY
        }

        /*
         * TRÆK DEN RØDE RAMME.
         *
         * Tryk inde i rammen = flyt rammen.
         * Tryk udenfor rammen = WebView kan scrolles.
         */
        var touchOffsetX =
            0f

        var touchOffsetY =
            0f

        frameOverlay.setOnTouchListener {
                view,
                event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    /*
                     * Fingerens placering inde i rammen.
                     */
                    touchOffsetX =
                        event.rawX -
                            view.x

                    touchOffsetY =
                        event.rawY -
                            view.y

                    /*
                     * Når man starter inde i rammen,
                     * skal WebView ikke overtage bevægelsen.
                     */
                    view.parent
                        .requestDisallowInterceptTouchEvent(
                            true
                        )

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val containerWidth =
                        websiteContainer.width
                            .toFloat()

                    val containerHeight =
                        websiteContainer.height
                            .toFloat()

                    val maxX =
                        (
                            containerWidth -
                                view.width
                            ).coerceAtLeast(
                                0f
                            )

                    val maxY =
                        (
                            containerHeight -
                                view.height
                            ).coerceAtLeast(
                                0f
                            )

                    /*
                     * Ny placering.
                     */
                    frameX =
                        (
                            event.rawX -
                                touchOffsetX
                            ).coerceIn(
                                0f,
                                maxX
                            )

                    frameY =
                        (
                            event.rawY -
                                touchOffsetY
                            ).coerceIn(
                                0f,
                                maxY
                            )

                    view.x =
                        frameX

                    view.y =
                        frameY

                    true
                }

                MotionEvent.ACTION_UP -> {

                    view.parent
                        .requestDisallowInterceptTouchEvent(
                            false
                        )

                    view.performClick()

                    true
                }

                MotionEvent.ACTION_CANCEL -> {

                    view.parent
                        .requestDisallowInterceptTouchEvent(
                            false
                        )

                    true
                }

                else ->
                    false
            }
        }

        /*
         * Gør performClick gyldig for accessibility.
         */
        frameOverlay.setOnClickListener {
            // Intet skal ske ved almindeligt klik.
        }

        val saveButton =
            Button(this).apply {

                text =
                    "Gem dette område"

                setOnClickListener {

                    /*
                     * Hvor langt brugeren har scrollet
                     * på selve hjemmesiden.
                     */
                    val scrollX =
                        webView.scrollX

                    val scrollY =
                        webView.scrollY

                    /*
                     * Den røde rammes placering på
                     * den synlige WebView.
                     */
                    val selectedScreenX =
                        scrollX +
                            frameOverlay.x

                    val selectedScreenY =
                        scrollY +
                            frameOverlay.y

                    /*
                     * Worker'en renderer siden med en
                     * bredde på widgetWidth * 3.
                     *
                     * Derfor konverterer vi WebViewens
                     * koordinater til workerens
                     * koordinatsystem.
                     */
                    val scaleToWorker =
                        (
                            widgetWidth *
                                3f
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

                        /*
                         * Gem også WebView-positionen,
                         * så område-vælgeren kan åbne
                         * samme sted næste gang.
                         */
                        .putInt(
                            "websiteScrollX_$appWidgetId",
                            scrollX
                        )
                        .putInt(
                            "websiteScrollY_$appWidgetId",
                            scrollY
                        )

                        /*
                         * Gem rammens position.
                         */
                        .putFloat(
                            "cropFrameX_$appWidgetId",
                            frameOverlay.x
                        )
                        .putFloat(
                            "cropFrameY_$appWidgetId",
                            frameOverlay.y
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

        /*
         * WebView.
         */
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
                     * Gå tilbage til tidligere
                     * scrollposition.
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

                    /*
                     * Hvis rammen tidligere er blevet
                     * flyttet, gendan dens position.
                     */
                    websiteContainer.postDelayed({

                        val savedFrameX =
                            prefs.getFloat(
                                "cropFrameX_$appWidgetId",
                                -1f
                            )

                        val savedFrameY =
                            prefs.getFloat(
                                "cropFrameY_$appWidgetId",
                                -1f
                            )

                        if (
                            savedFrameX >= 0f &&
                            savedFrameY >= 0f
                        ) {

                            val maxX =
                                (
                                    websiteContainer.width -
                                        frameOverlay.width
                                    ).coerceAtLeast(
                                        0
                                    ).toFloat()

                            val maxY =
                                (
                                    websiteContainer.height -
                                        frameOverlay.height
                                    ).coerceAtLeast(
                                        0
                                    ).toFloat()

                            frameX =
                                savedFrameX.coerceIn(
                                    0f,
                                    maxX
                                )

                            frameY =
                                savedFrameY.coerceIn(
                                    0f,
                                    maxY
                                )

                            frameOverlay.x =
                                frameX

                            frameOverlay.y =
                                frameY
                        }

                    }, 1100)
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
