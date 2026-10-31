package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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

                textSize = 16f

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

                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadsImagesAutomatically = true

                    useWideViewPort = true
                    loadWithOverviewMode = false

                    builtInZoomControls = false
                    displayZoomControls = false
                }

                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = true

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

        val frameOverlay =
            View(this).apply {

                isClickable = true
                isFocusable = true

                elevation = 20f

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

        websiteContainer.addView(
            frameOverlay,
            FrameLayout.LayoutParams(
                1,
                1
            )
        )

        websiteContainer.post {

            val availableWidth =
                websiteContainer.width
                    .coerceAtLeast(1)

            val availableHeight =
                websiteContainer.height
                    .coerceAtLeast(1)

            var frameWidth =
                (availableWidth * 0.70f)
                    .roundToInt()
                    .coerceAtLeast(1)

            var frameHeight =
                (
                    frameWidth *
                        widgetRatio
                ).roundToInt()
                    .coerceAtLeast(1)

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

            val maxX =
                (
                    availableWidth -
                        frameWidth
                ).coerceAtLeast(0)
                    .toFloat()

            val maxY =
                (
                    availableHeight -
                        frameHeight
                ).coerceAtLeast(0)
                    .toFloat()

            if (
                savedFrameX >= 0f &&
                savedFrameY >= 0f
            ) {

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

            } else {

                frameX =
                    maxX / 2f

                frameY =
                    maxY / 2f
            }

            frameOverlay.x = frameX
            frameOverlay.y = frameY
        }

        var touchOffsetX = 0f
        var touchOffsetY = 0f

        frameOverlay.setOnTouchListener {
                view,
                event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    touchOffsetX =
                        event.rawX -
                            view.x

                    touchOffsetY =
                        event.rawY -
                            view.y

                    view.parent
                        .requestDisallowInterceptTouchEvent(
                            true
                        )

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val maxX =
                        (
                            websiteContainer.width -
                                view.width
                        ).coerceAtLeast(0)
                            .toFloat()

                    val maxY =
                        (
                            websiteContainer.height -
                                view.height
                        ).coerceAtLeast(0)
                            .toFloat()

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

                    view.x = frameX
                    view.y = frameY

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

                else -> false
            }
        }

        frameOverlay.setOnClickListener {
            // Intet ved almindeligt klik.
        }

        root.addView(
            Button(this).apply {

                text =
                    "Gem dette område"

                setOnClickListener {

                    val viewportWidth =
                        webView.width
                            .coerceAtLeast(1)

                    val viewportHeight =
                        webView.height
                            .coerceAtLeast(1)

                    /*
                     * Den faktiske WebView-scale.
                     */
                    val webScale =
                        webView.scale
                            .coerceAtLeast(0.01f)

                    val scrollX =
                        webView.scrollX

                    val scrollY =
                        webView.scrollY

                    /*
                     * scrollX/scrollY og rammens
                     * position er i skærmpixels.
                     *
                     * Vi gemmer derfor både den
                     * præcise pixelposition OG
                     * dokumentpositionen korrigeret
                     * for WebViewens scale.
                     */
                    val screenX =
                        scrollX +
                            frameOverlay.x

                    val screenY =
                        scrollY +
                            frameOverlay.y

                    val documentX =
                        (
                            screenX /
                                webScale
                        ).roundToInt()
                            .coerceAtLeast(0)

                    val documentY =
                        (
                            screenY /
                                webScale
                        ).roundToInt()
                            .coerceAtLeast(0)

                    val selectionWidth =
                        frameOverlay.width
                            .coerceAtLeast(1)

                    val selectionHeight =
                        frameOverlay.height
                            .coerceAtLeast(1)

                    /*
                     * Rammens størrelse i samme
                     * dokumentkoordinater.
                     */
                    val documentSelectionWidth =
                        (
                            selectionWidth /
                                webScale
                        ).roundToInt()
                            .coerceAtLeast(1)

                    val documentSelectionHeight =
                        (
                            selectionHeight /
                                webScale
                        ).roundToInt()
                            .coerceAtLeast(1)

                    prefs.edit()

                        /*
                         * Dokumentkoordinater.
                         */
                        .putInt(
                            "cropDocumentX_$appWidgetId",
                            documentX
                        )

                        .putInt(
                            "cropDocumentY_$appWidgetId",
                            documentY
                        )

                        .putInt(
                            "cropDocumentSelectionWidth_$appWidgetId",
                            documentSelectionWidth
                        )

                        .putInt(
                            "cropDocumentSelectionHeight_$appWidgetId",
                            documentSelectionHeight
                        )

                        /*
                         * VIGTIGT:
                         * Gem den faktiske scale.
                         */
                        .putFloat(
                            "cropWebScale_$appWidgetId",
                            webScale
                        )

                        /*
                         * Viewport.
                         */
                        .putInt(
                            "cropViewportWidth_$appWidgetId",
                            viewportWidth
                        )

                        .putInt(
                            "cropViewportHeight_$appWidgetId",
                            viewportHeight
                        )

                        /*
                         * Rammens almindelige
                         * pixelstørrelse beholdes også.
                         */
                        .putInt(
                            "cropSelectionWidth_$appWidgetId",
                            selectionWidth
                        )

                        .putInt(
                            "cropSelectionHeight_$appWidgetId",
                            selectionHeight
                        )

                        /*
                         * Til gendannelse i vælgeren.
                         */
                        .putInt(
                            "websiteScrollX_$appWidgetId",
                            scrollX
                        )

                        .putInt(
                            "websiteScrollY_$appWidgetId",
                            scrollY
                        )

                        .putFloat(
                            "cropFrameX_$appWidgetId",
                            frameOverlay.x
                        )

                        .putFloat(
                            "cropFrameY_$appWidgetId",
                            frameOverlay.y
                        )

                        .putBoolean(
                            "cropEnabled_$appWidgetId",
                            true
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
