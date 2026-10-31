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

        val density =
            resources.displayMetrics.density

        /*
         * Ca. størrelse på ét widget-felt.
         *
         * Det er rammens mindste størrelse.
         * Bredde og højde kan derefter ændres
         * helt uafhængigt.
         */
        val minFrameWidth =
            (70f * density)
                .roundToInt()
                .coerceAtLeast(70)

        val minFrameHeight =
            (70f * density)
                .roundToInt()
                .coerceAtLeast(70)

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
                    "Scroll på hjemmesiden. Flyt den røde ramme eller træk frit i et af de 4 hjørner."

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

        /*
         * Selve den røde ramme.
         */
        val frameContainer =
            FrameLayout(this).apply {
                elevation = 20f
            }

        websiteContainer.addView(
            frameContainer,
            FrameLayout.LayoutParams(
                1,
                1
            )
        )

        val frameOverlay =
            View(this).apply {

                isClickable = true
                isFocusable = true

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

        frameContainer.addView(
            frameOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        /*
         * De fire hjørner.
         */
        val handleSize =
            (26f * density)
                .roundToInt()
                .coerceAtLeast(26)

        fun createHandle(): View {

            return View(this).apply {

                elevation = 30f

                background =
                    GradientDrawable().apply {

                        shape =
                            GradientDrawable.OVAL

                        setColor(
                            Color.WHITE
                        )

                        setStroke(
                            (3f * density)
                                .roundToInt()
                                .coerceAtLeast(3),
                            Color.RED
                        )
                    }
            }
        }

        val topLeft =
            createHandle()

        val topRight =
            createHandle()

        val bottomLeft =
            createHandle()

        val bottomRight =
            createHandle()

        listOf(
            topLeft,
            topRight,
            bottomLeft,
            bottomRight
        ).forEach {

            websiteContainer.addView(
                it,
                FrameLayout.LayoutParams(
                    handleSize,
                    handleSize
                )
            )
        }

        /*
         * Placér håndtagene præcis i
         * rammens fire hjørner.
         */
        fun updateHandles() {

            val half =
                handleSize / 2f

            topLeft.x =
                frameContainer.x - half

            topLeft.y =
                frameContainer.y - half

            topRight.x =
                frameContainer.x +
                    frameContainer.width -
                    half

            topRight.y =
                frameContainer.y - half

            bottomLeft.x =
                frameContainer.x - half

            bottomLeft.y =
                frameContainer.y +
                    frameContainer.height -
                    half

            bottomRight.x =
                frameContainer.x +
                    frameContainer.width -
                    half

            bottomRight.y =
                frameContainer.y +
                    frameContainer.height -
                    half
        }

        /*
         * Sæt position OG størrelse.
         *
         * Der er INGEN låst proportion længere.
         */
        fun setFrame(
            x: Float,
            y: Float,
            width: Int,
            height: Int
        ) {

            val areaWidth =
                websiteContainer.width
                    .coerceAtLeast(1)

            val areaHeight =
                websiteContainer.height
                    .coerceAtLeast(1)

            var newWidth =
                width.coerceAtLeast(
                    minFrameWidth
                )

            var newHeight =
                height.coerceAtLeast(
                    minFrameHeight
                )

            newWidth =
                newWidth.coerceAtMost(
                    areaWidth
                )

            newHeight =
                newHeight.coerceAtMost(
                    areaHeight
                )

            val maxX =
                (
                    areaWidth -
                        newWidth
                ).coerceAtLeast(0)
                    .toFloat()

            val maxY =
                (
                    areaHeight -
                        newHeight
                ).coerceAtLeast(0)
                    .toFloat()

            val newX =
                x.coerceIn(
                    0f,
                    maxX
                )

            val newY =
                y.coerceIn(
                    0f,
                    maxY
                )

            frameContainer.layoutParams =
                FrameLayout.LayoutParams(
                    newWidth,
                    newHeight
                )

            frameContainer.x =
                newX

            frameContainer.y =
                newY

            frameContainer.post {
                updateHandles()
            }
        }

        /*
         * Første størrelse.
         *
         * Hvis vi allerede har gemt en størrelse,
         * bruger vi den igen.
         */
        websiteContainer.post {

            val areaWidth =
                websiteContainer.width
                    .coerceAtLeast(1)

            val areaHeight =
                websiteContainer.height
                    .coerceAtLeast(1)

            var frameWidth =
                prefs.getInt(
                    "cropFrameWidth_$appWidgetId",
                    0
                )

            var frameHeight =
                prefs.getInt(
                    "cropFrameHeight_$appWidgetId",
                    0
                )

            if (frameWidth <= 0) {

                frameWidth =
                    (areaWidth * 0.55f)
                        .roundToInt()
            }

            if (frameHeight <= 0) {

                frameHeight =
                    (areaHeight * 0.30f)
                        .roundToInt()
            }

            frameWidth =
                frameWidth.coerceIn(
                    minFrameWidth,
                    areaWidth
                )

            frameHeight =
                frameHeight.coerceIn(
                    minFrameHeight,
                    areaHeight
                )

            val savedX =
                prefs.getFloat(
                    "cropFrameX_$appWidgetId",
                    -1f
                )

            val savedY =
                prefs.getFloat(
                    "cropFrameY_$appWidgetId",
                    -1f
                )

            val startX =
                if (savedX >= 0f) {

                    savedX

                } else {

                    (
                        areaWidth -
                            frameWidth
                    ) / 2f
                }

            val startY =
                if (savedY >= 0f) {

                    savedY

                } else {

                    (
                        areaHeight -
                            frameHeight
                    ) / 2f
                }

            setFrame(
                startX,
                startY,
                frameWidth,
                frameHeight
            )
        }

        /*
         * Træk inde i rammen =
         * flyt hele rammen.
         */
        var moveStartRawX = 0f
        var moveStartRawY = 0f

        var moveStartX = 0f
        var moveStartY = 0f

        frameOverlay.setOnTouchListener {
                view,
                event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    moveStartRawX =
                        event.rawX

                    moveStartRawY =
                        event.rawY

                    moveStartX =
                        frameContainer.x

                    moveStartY =
                        frameContainer.y

                    websiteContainer
                        .requestDisallowInterceptTouchEvent(
                            true
                        )

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val newX =
                        moveStartX +
                            (
                                event.rawX -
                                    moveStartRawX
                            )

                    val newY =
                        moveStartY +
                            (
                                event.rawY -
                                    moveStartRawY
                            )

                    setFrame(
                        newX,
                        newY,
                        frameContainer.width,
                        frameContainer.height
                    )

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    websiteContainer
                        .requestDisallowInterceptTouchEvent(
                            false
                        )

                    true
                }

                else -> false
            }
        }

        /*
         * Hjørnetyper.
         */
        val TOP_LEFT = 0
        val TOP_RIGHT = 1
        val BOTTOM_LEFT = 2
        val BOTTOM_RIGHT = 3

        /*
         * Fri resize.
         *
         * Modsatte hjørne bliver stående.
         * Bredde og højde ændres uafhængigt.
         */
        fun installResizeHandle(
            handle: View,
            corner: Int
        ) {

            var fixedX = 0f
            var fixedY = 0f

            handle.setOnTouchListener {
                    view,
                    event ->

                when (event.actionMasked) {

                    MotionEvent.ACTION_DOWN -> {

                        when (corner) {

                            TOP_LEFT -> {

                                fixedX =
                                    frameContainer.x +
                                        frameContainer.width

                                fixedY =
                                    frameContainer.y +
                                        frameContainer.height
                            }

                            TOP_RIGHT -> {

                                fixedX =
                                    frameContainer.x

                                fixedY =
                                    frameContainer.y +
                                        frameContainer.height
                            }

                            BOTTOM_LEFT -> {

                                fixedX =
                                    frameContainer.x +
                                        frameContainer.width

                                fixedY =
                                    frameContainer.y
                            }

                            BOTTOM_RIGHT -> {

                                fixedX =
                                    frameContainer.x

                                fixedY =
                                    frameContainer.y
                            }
                        }

                        websiteContainer
                            .requestDisallowInterceptTouchEvent(
                                true
                            )

                        true
                    }

                    MotionEvent.ACTION_MOVE -> {

                        val location =
                            IntArray(2)

                        websiteContainer
                            .getLocationOnScreen(
                                location
                            )

                        val touchX =
                            event.rawX -
                                location[0]

                        val touchY =
                            event.rawY -
                                location[1]

                        var newX: Float
                        var newY: Float

                        var newWidth: Int
                        var newHeight: Int

                        when (corner) {

                            TOP_LEFT -> {

                                newWidth =
                                    (
                                        fixedX -
                                            touchX
                                    ).roundToInt()

                                newHeight =
                                    (
                                        fixedY -
                                            touchY
                                    ).roundToInt()

                                newWidth =
                                    newWidth.coerceAtLeast(
                                        minFrameWidth
                                    )

                                newHeight =
                                    newHeight.coerceAtLeast(
                                        minFrameHeight
                                    )

                                newX =
                                    fixedX -
                                        newWidth

                                newY =
                                    fixedY -
                                        newHeight
                            }

                            TOP_RIGHT -> {

                                newWidth =
                                    (
                                        touchX -
                                            fixedX
                                    ).roundToInt()

                                newHeight =
                                    (
                                        fixedY -
                                            touchY
                                    ).roundToInt()

                                newWidth =
                                    newWidth.coerceAtLeast(
                                        minFrameWidth
                                    )

                                newHeight =
                                    newHeight.coerceAtLeast(
                                        minFrameHeight
                                    )

                                newX =
                                    fixedX

                                newY =
                                    fixedY -
                                        newHeight
                            }

                            BOTTOM_LEFT -> {

                                newWidth =
                                    (
                                        fixedX -
                                            touchX
                                    ).roundToInt()

                                newHeight =
                                    (
                                        touchY -
                                            fixedY
                                    ).roundToInt()

                                newWidth =
                                    newWidth.coerceAtLeast(
                                        minFrameWidth
                                    )

                                newHeight =
                                    newHeight.coerceAtLeast(
                                        minFrameHeight
                                    )

                                newX =
                                    fixedX -
                                        newWidth

                                newY =
                                    fixedY
                            }

                            else -> {

                                newWidth =
                                    (
                                        touchX -
                                            fixedX
                                    ).roundToInt()

                                newHeight =
                                    (
                                        touchY -
                                            fixedY
                                    ).roundToInt()

                                newWidth =
                                    newWidth.coerceAtLeast(
                                        minFrameWidth
                                    )

                                newHeight =
                                    newHeight.coerceAtLeast(
                                        minFrameHeight
                                    )

                                newX =
                                    fixedX

                                newY =
                                    fixedY
                            }
                        }

                        /*
                         * Begræns mod skærmens kanter.
                         * Bredde og højde forbliver frie.
                         */
                        val areaWidth =
                            websiteContainer.width
                                .coerceAtLeast(1)

                        val areaHeight =
                            websiteContainer.height
                                .coerceAtLeast(1)

                        if (newX < 0f) {

                            newWidth =
                                fixedX
                                    .roundToInt()
                                    .coerceAtLeast(
                                        minFrameWidth
                                    )

                            newX = 0f
                        }

                        if (newY < 0f) {

                            newHeight =
                                fixedY
                                    .roundToInt()
                                    .coerceAtLeast(
                                        minFrameHeight
                                    )

                            newY = 0f
                        }

                        if (
                            newX +
                                newWidth >
                            areaWidth
                        ) {

                            newWidth =
                                (
                                    areaWidth -
                                        newX
                                ).roundToInt()
                                    .coerceAtLeast(
                                        minFrameWidth
                                    )
                        }

                        if (
                            newY +
                                newHeight >
                            areaHeight
                        ) {

                            newHeight =
                                (
                                    areaHeight -
                                        newY
                                ).roundToInt()
                                    .coerceAtLeast(
                                        minFrameHeight
                                    )
                        }

                        setFrame(
                            newX,
                            newY,
                            newWidth,
                            newHeight
                        )

                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {

                        websiteContainer
                            .requestDisallowInterceptTouchEvent(
                                false
                            )

                        true
                    }

                    else -> false
                }
            }
        }

        installResizeHandle(
            topLeft,
            TOP_LEFT
        )

        installResizeHandle(
            topRight,
            TOP_RIGHT
        )

        installResizeHandle(
            bottomLeft,
            BOTTOM_LEFT
        )

        installResizeHandle(
            bottomRight,
            BOTTOM_RIGHT
        )

        /*
         * GEM.
         *
         * Den fungerende CSS-metode ændres IKKE.
         */
        root.addView(
            Button(this).apply {

                text =
                    "Gem dette område"

                setOnClickListener {

                    val javascript =
                        """
                        (function() {
                            return [
                                window.scrollX || window.pageXOffset || 0,
                                window.scrollY || window.pageYOffset || 0,
                                window.innerWidth || document.documentElement.clientWidth || 1,
                                window.innerHeight || document.documentElement.clientHeight || 1,
                                document.documentElement.scrollWidth || 1,
                                Math.max(
                                    document.body ? document.body.scrollHeight : 0,
                                    document.documentElement.scrollHeight || 0
                                )
                            ].join(",");
                        })();
                        """.trimIndent()

                    webView.evaluateJavascript(
                        javascript
                    ) { result ->

                        try {

                            val cleaned =
                                result
                                    .trim()
                                    .removePrefix("\"")
                                    .removeSuffix("\"")
                                    .replace("\\\"", "\"")

                            val values =
                                cleaned.split(",")

                            if (values.size < 6) {

                                Toast.makeText(
                                    this@CropWebsiteActivity,
                                    "Kunne ikke aflæse hjemmesidens position.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@evaluateJavascript
                            }

                            val jsScrollX =
                                values[0]
                                    .toFloatOrNull()
                                    ?: 0f

                            val jsScrollY =
                                values[1]
                                    .toFloatOrNull()
                                    ?: 0f

                            val jsViewportWidth =
                                values[2]
                                    .toFloatOrNull()
                                    ?.coerceAtLeast(1f)
                                    ?: 1f

                            val jsViewportHeight =
                                values[3]
                                    .toFloatOrNull()
                                    ?.coerceAtLeast(1f)
                                    ?: 1f

                            val jsDocumentWidth =
                                values[4]
                                    .toFloatOrNull()
                                    ?.coerceAtLeast(
                                        jsViewportWidth
                                    )
                                    ?: jsViewportWidth

                            val jsDocumentHeight =
                                values[5]
                                    .toFloatOrNull()
                                    ?.coerceAtLeast(
                                        jsViewportHeight
                                    )
                                    ?: jsViewportHeight

                            val pixelsPerCssX =
                                webView.width.toFloat() /
                                    jsViewportWidth

                            val pixelsPerCssY =
                                webView.height.toFloat() /
                                    jsViewportHeight

                            val frameCssX =
                                frameContainer.x /
                                    pixelsPerCssX

                            val frameCssY =
                                frameContainer.y /
                                    pixelsPerCssY

                            val cropCssX =
                                jsScrollX +
                                    frameCssX

                            val cropCssY =
                                jsScrollY +
                                    frameCssY

                            val cropCssWidth =
                                frameContainer.width /
                                    pixelsPerCssX

                            val cropCssHeight =
                                frameContainer.height /
                                    pixelsPerCssY

                            val webScale =
                                webView.scale
                                    .coerceAtLeast(0.01f)

                            prefs.edit()

                                .putFloat(
                                    "cropCssX_$appWidgetId",
                                    cropCssX
                                )

                                .putFloat(
                                    "cropCssY_$appWidgetId",
                                    cropCssY
                                )

                                .putFloat(
                                    "cropCssWidth_$appWidgetId",
                                    cropCssWidth
                                )

                                .putFloat(
                                    "cropCssHeight_$appWidgetId",
                                    cropCssHeight
                                )

                                .putFloat(
                                    "cropCssViewportWidth_$appWidgetId",
                                    jsViewportWidth
                                )

                                .putFloat(
                                    "cropCssViewportHeight_$appWidgetId",
                                    jsViewportHeight
                                )

                                .putFloat(
                                    "cropCssDocumentWidth_$appWidgetId",
                                    jsDocumentWidth
                                )

                                .putFloat(
                                    "cropCssDocumentHeight_$appWidgetId",
                                    jsDocumentHeight
                                )

                                .putInt(
                                    "cropViewportWidth_$appWidgetId",
                                    webView.width
                                        .coerceAtLeast(1)
                                )

                                .putInt(
                                    "cropViewportHeight_$appWidgetId",
                                    webView.height
                                        .coerceAtLeast(1)
                                )

                                .putFloat(
                                    "cropWebScale_$appWidgetId",
                                    webScale
                                )

                                .putInt(
                                    "cropSelectionWidth_$appWidgetId",
                                    frameContainer.width
                                        .coerceAtLeast(1)
                                )

                                .putInt(
                                    "cropSelectionHeight_$appWidgetId",
                                    frameContainer.height
                                        .coerceAtLeast(1)
                                )

                                .putInt(
                                    "cropFrameWidth_$appWidgetId",
                                    frameContainer.width
                                )

                                .putInt(
                                    "cropFrameHeight_$appWidgetId",
                                    frameContainer.height
                                )

                                .putInt(
                                    "websiteScrollX_$appWidgetId",
                                    webView.scrollX
                                )

                                .putInt(
                                    "websiteScrollY_$appWidgetId",
                                    webView.scrollY
                                )

                                .putFloat(
                                    "cropFrameX_$appWidgetId",
                                    frameContainer.x
                                )

                                .putFloat(
                                    "cropFrameY_$appWidgetId",
                                    frameContainer.y
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

                        } catch (_: Exception) {

                            Toast.makeText(
                                this@CropWebsiteActivity,
                                "Kunne ikke gemme området.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )

        setContentView(root)

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

        webView.loadUrl(address)
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
