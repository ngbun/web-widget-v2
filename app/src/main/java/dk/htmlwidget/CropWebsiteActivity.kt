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

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
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
            manager.getAppWidgetOptions(appWidgetId)

        val density =
            resources.displayMetrics.density

        /*
         * Widgettens aktuelle størrelse.
         *
         * Vi bruger både MIN og MAX og tager
         * den største gyldige værdi.
         */
        val minWidthDp =
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                300
            )

        val maxWidthDp =
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                minWidthDp
            )

        val minHeightDp =
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                180
            )

        val maxHeightDp =
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                minHeightDp
            )

        val widgetWidth =
            (
                maxOf(
                    minWidthDp,
                    maxWidthDp
                ) * density
            ).roundToInt()
                .coerceAtLeast(1)

        val widgetHeight =
            (
                maxOf(
                    minHeightDp,
                    maxHeightDp
                ) * density
            ).roundToInt()
                .coerceAtLeast(1)

        /*
         * Dette forhold skal den røde ramme
         * altid beholde.
         */
        val widgetRatio =
            widgetHeight.toFloat() /
                widgetWidth.toFloat()

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(Color.WHITE)
            }

        root.addView(
            TextView(this).apply {

                text =
                    "Scroll på hjemmesiden. Flyt den røde ramme eller træk i et af de 4 hjørner for at ændre størrelsen."

                textSize = 16f

                setTextColor(Color.BLACK)

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

                setBackgroundColor(Color.WHITE)

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
         * Container til selve rammen + de fire
         * håndtag.
         */
        val frameContainer =
            FrameLayout(this).apply {
                elevation = 20f
            }

        websiteContainer.addView(
            frameContainer,
            FrameLayout.LayoutParams(1, 1)
        )

        /*
         * Selve den røde ramme.
         */
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
         * Hjørnehåndtag.
         */
        val handleSize =
            (28f * density)
                .roundToInt()
                .coerceAtLeast(28)

        fun createHandle(): View {

            return View(this).apply {

                elevation = 30f

                background =
                    GradientDrawable().apply {

                        shape =
                            GradientDrawable.OVAL

                        setColor(Color.WHITE)

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

        websiteContainer.addView(
            topLeft,
            FrameLayout.LayoutParams(
                handleSize,
                handleSize
            )
        )

        websiteContainer.addView(
            topRight,
            FrameLayout.LayoutParams(
                handleSize,
                handleSize
            )
        )

        websiteContainer.addView(
            bottomLeft,
            FrameLayout.LayoutParams(
                handleSize,
                handleSize
            )
        )

        websiteContainer.addView(
            bottomRight,
            FrameLayout.LayoutParams(
                handleSize,
                handleSize
            )
        )

        /*
         * Flyt håndtagene med rammen.
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
         * Opdater rammens position og størrelse.
         */
        fun setFrame(
            x: Float,
            y: Float,
            width: Int
        ) {

            val containerWidth =
                websiteContainer.width
                    .coerceAtLeast(1)

            val containerHeight =
                websiteContainer.height
                    .coerceAtLeast(1)

            val minWidth =
                (100f * density)
                    .roundToInt()
                    .coerceAtLeast(80)

            var newWidth =
                width.coerceAtLeast(minWidth)

            var newHeight =
                (
                    newWidth *
                        widgetRatio
                ).roundToInt()
                    .coerceAtLeast(1)

            /*
             * Rammen må ikke blive større
             * end skærmområdet.
             */
            if (newWidth > containerWidth) {

                newWidth =
                    containerWidth

                newHeight =
                    (
                        newWidth *
                            widgetRatio
                    ).roundToInt()
            }

            if (newHeight > containerHeight) {

                newHeight =
                    containerHeight

                newWidth =
                    (
                        newHeight /
                            widgetRatio
                    ).roundToInt()
            }

            val maxX =
                (
                    containerWidth -
                        newWidth
                ).coerceAtLeast(0)
                    .toFloat()

            val maxY =
                (
                    containerHeight -
                        newHeight
                ).coerceAtLeast(0)
                    .toFloat()

            frameX =
                x.coerceIn(
                    0f,
                    maxX
                )

            frameY =
                y.coerceIn(
                    0f,
                    maxY
                )

            frameContainer.layoutParams =
                FrameLayout.LayoutParams(
                    newWidth,
                    newHeight
                )

            frameContainer.x = frameX
            frameContainer.y = frameY

            frameContainer.post {
                updateHandles()
            }
        }

        websiteContainer.post {

            val availableWidth =
                websiteContainer.width
                    .coerceAtLeast(1)

            val availableHeight =
                websiteContainer.height
                    .coerceAtLeast(1)

            /*
             * Hvis brugeren tidligere har ændret
             * størrelsen, bruger vi den igen.
             */
            var frameWidth =
                prefs.getInt(
                    "cropFrameWidth_$appWidgetId",
                    0
                )

            if (frameWidth <= 0) {
                frameWidth =
                    (availableWidth * 0.70f)
                        .roundToInt()
            }

            var frameHeight =
                (
                    frameWidth *
                        widgetRatio
                ).roundToInt()

            if (frameHeight > availableHeight * 0.75f) {

                frameHeight =
                    (availableHeight * 0.75f)
                        .roundToInt()

                frameWidth =
                    (
                        frameHeight /
                            widgetRatio
                    ).roundToInt()
            }

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

            val startX =
                if (savedFrameX >= 0f) {

                    savedFrameX

                } else {

                    (
                        availableWidth -
                            frameWidth
                    ) / 2f
                }

            val startY =
                if (savedFrameY >= 0f) {

                    savedFrameY

                } else {

                    (
                        availableHeight -
                            frameHeight
                    ) / 2f
                }

            setFrame(
                startX,
                startY,
                frameWidth
            )
        }

        /*
         * Flyt hele rammen ved at trække
         * inde i den.
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

                    view.parent.parent
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
                        frameContainer.width
                    )

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    view.parent.parent
                        .requestDisallowInterceptTouchEvent(
                            false
                        )

                    true
                }

                else -> false
            }
        }

        /*
         * De fire hjørner.
         *
         * Modsatte hjørne holdes fast, og
         * widgettens proportion bevares.
         */
        fun installResizeHandle(
            handle: View,
            corner: Int
        ) {

            var anchorX = 0f
            var anchorY = 0f

            handle.setOnTouchListener {
                    view,
                    event ->

                when (event.actionMasked) {

                    MotionEvent.ACTION_DOWN -> {

                        when (corner) {

                            0 -> {
                                // Top venstre.
                                anchorX =
                                    frameContainer.x +
                                        frameContainer.width

                                anchorY =
                                    frameContainer.y +
                                        frameContainer.height
                            }

                            1 -> {
                                // Top højre.
                                anchorX =
                                    frameContainer.x

                                anchorY =
                                    frameContainer.y +
                                        frameContainer.height
                            }

                            2 -> {
                                // Bund venstre.
                                anchorX =
                                    frameContainer.x +
                                        frameContainer.width

                                anchorY =
                                    frameContainer.y
                            }

                            else -> {
                                // Bund højre.
                                anchorX =
                                    frameContainer.x

                                anchorY =
                                    frameContainer.y
                            }
                        }

                        view.parent
                            .requestDisallowInterceptTouchEvent(
                                true
                            )

                        true
                    }

                    MotionEvent.ACTION_MOVE -> {

                        val location =
                            IntArray(2)

                        websiteContainer
                            .getLocationOnScreen(location)

                        val touchX =
                            event.rawX -
                                location[0]

                        val touchY =
                            event.rawY -
                                location[1]

                        /*
                         * Beregn ønsket bredde ud
                         * fra både X og Y.
                         */
                        val widthFromX =
                            when (corner) {

                                0, 2 ->
                                    anchorX -
                                        touchX

                                else ->
                                    touchX -
                                        anchorX
                            }

                        val heightFromY =
                            when (corner) {

                                0, 1 ->
                                    anchorY -
                                        touchY

                                else ->
                                    touchY -
                                        anchorY
                            }

                        val widthFromY =
                            heightFromY /
                                widgetRatio

                        /*
                         * Brug den største bevægelse,
                         * så resize føles naturlig.
                         */
                        val requestedWidth =
                            if (
                                kotlin.math.abs(
                                    widthFromX -
                                        frameContainer.width
                                ) >=
                                kotlin.math.abs(
                                    widthFromY -
                                        frameContainer.width
                                )
                            ) {
                                widthFromX
                            } else {
                                widthFromY
                            }

                        val newWidth =
                            requestedWidth
                                .roundToInt()
                                .coerceAtLeast(
                                    (100f * density)
                                        .roundToInt()
                                )

                        val newHeight =
                            (
                                newWidth *
                                    widgetRatio
                            ).roundToInt()

                        val newX =
                            when (corner) {

                                0, 2 ->
                                    anchorX -
                                        newWidth

                                else ->
                                    anchorX
                            }

                        val newY =
                            when (corner) {

                                0, 1 ->
                                    anchorY -
                                        newHeight

                                else ->
                                    anchorY
                            }

                        setFrame(
                            newX,
                            newY,
                            newWidth
                        )

                        true
                    }

                    MotionEvent.ACTION_UP,
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
        }

        installResizeHandle(
            topLeft,
            0
        )

        installResizeHandle(
            topRight,
            1
        )

        installResizeHandle(
            bottomLeft,
            2
        )

        installResizeHandle(
            bottomRight,
            3
        )

        root.addView(
            Button(this).apply {

                text =
                    "Gem dette område"

                setOnClickListener {

                    /*
                     * DEN FUNGERENDE CSS-METODE
                     * BEHOLDES.
                     */
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

                            /*
                             * VIGTIGT:
                             * Nu bruges frameContainer,
                             * fordi det er den ramme,
                             * brugeren faktisk flytter
                             * og ændrer størrelse på.
                             */
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

                                /*
                                 * Gem også brugerens nye
                                 * rammestørrelse.
                                 */
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
                                host.endsWith(".$it")
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
