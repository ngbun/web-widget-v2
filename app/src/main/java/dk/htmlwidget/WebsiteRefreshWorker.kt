package dk.htmlwidget

import android.app.Presentation
import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RemoteViews
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class WebsiteRefreshWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val mainHandler =
        Handler(Looper.getMainLooper())

    override fun doWork(): Result {

        val appWidgetId =
            inputData.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            return Result.success()
        }

        val prefs =
            applicationContext.getSharedPreferences(
                "settings",
                Context.MODE_PRIVATE
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
            return Result.success()
        }

        val latch =
            CountDownLatch(1)

        var successful = false

        mainHandler.post {

            successful =
                renderWebsite(
                    appWidgetId,
                    address,
                    latch
                )
        }

        val completed =
            latch.await(
                30,
                TimeUnit.SECONDS
            )

        return if (
            completed &&
            successful
        ) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun renderWebsite(
        appWidgetId: Int,
        address: String,
        latch: CountDownLatch
    ): Boolean {

        val context =
            applicationContext

        val prefs =
            context.getSharedPreferences(
                "settings",
                Context.MODE_PRIVATE
            )

        val cropEnabled =
            prefs.getBoolean(
                "cropEnabled_$appWidgetId",
                false
            )

        /*
         * NYE CSS-koordinater fra hjemmesiden.
         */
        val cropCssX =
            prefs.getFloat(
                "cropCssX_$appWidgetId",
                0f
            ).coerceAtLeast(0f)

        val cropCssY =
            prefs.getFloat(
                "cropCssY_$appWidgetId",
                0f
            ).coerceAtLeast(0f)

        val cropCssWidth =
            prefs.getFloat(
                "cropCssWidth_$appWidgetId",
                0f
            ).coerceAtLeast(0f)

        val cropCssHeight =
            prefs.getFloat(
                "cropCssHeight_$appWidgetId",
                0f
            ).coerceAtLeast(0f)

        /*
         * Viewport fra område-vælgeren.
         *
         * Worker'en oprettes med samme størrelse,
         * så hjemmesidens responsive layout bliver
         * så identisk som muligt.
         */
        val savedViewportWidth =
            prefs.getInt(
                "cropViewportWidth_$appWidgetId",
                0
            )

        val savedViewportHeight =
            prefs.getInt(
                "cropViewportHeight_$appWidgetId",
                0
            )

        val manager =
            AppWidgetManager.getInstance(
                context
            )

        val options =
            manager.getAppWidgetOptions(
                appWidgetId
            )

        val density =
            context.resources
                .displayMetrics
                .density

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
         * Samme viewport som område-vælgeren.
         */
        val renderWidth =
            if (
                cropEnabled &&
                savedViewportWidth > 0
            ) {
                savedViewportWidth
            } else {
                widgetWidth * 3
            }.coerceAtLeast(1)

        val renderHeight =
            if (
                cropEnabled &&
                savedViewportHeight > 0
            ) {
                savedViewportHeight
            } else {
                widgetHeight * 3
            }.coerceAtLeast(1)

        val displayManager =
            context.getSystemService(
                Context.DISPLAY_SERVICE
            ) as DisplayManager

        val surfaceTexture =
            SurfaceTexture(0)

        surfaceTexture.setDefaultBufferSize(
            renderWidth,
            renderHeight
        )

        val surface =
            Surface(surfaceTexture)

        var virtualDisplay: VirtualDisplay? =
            null

        var presentation: Presentation? =
            null

        var web: WebView? =
            null

        var completed = false

        fun cleanup() {

            if (completed) {
                return
            }

            completed = true

            try {
                web?.stopLoading()
                web?.destroy()
            } catch (_: Exception) {
            }

            try {
                presentation?.dismiss()
            } catch (_: Exception) {
            }

            try {
                virtualDisplay?.release()
            } catch (_: Exception) {
            }

            try {
                surface.release()
            } catch (_: Exception) {
            }

            try {
                surfaceTexture.release()
            } catch (_: Exception) {
            }

            latch.countDown()
        }

        /*
         * Tegner widgetten, når workerens egne
         * CSS viewport-værdier er aflæst.
         */
        fun drawWidget(
            workerCssViewportWidth: Float,
            workerCssViewportHeight: Float
        ) {

            if (completed) {
                return
            }

            try {

                val currentWeb =
                    web
                        ?: run {
                            cleanup()
                            return
                        }

                /*
                 * Hvor mange Android/WebView-pixels
                 * svarer til én CSS-pixel i worker'en?
                 */
                val pixelsPerCssX =
                    renderWidth.toFloat() /
                        workerCssViewportWidth
                            .coerceAtLeast(1f)

                val pixelsPerCssY =
                    renderHeight.toFloat() /
                        workerCssViewportHeight
                            .coerceAtLeast(1f)

                /*
                 * CSS-koordinaterne fra vælgeren
                 * omregnes direkte til workerens
                 * tegningskoordinater.
                 */
                val drawX =
                    if (cropEnabled) {

                        (
                            cropCssX *
                                pixelsPerCssX
                        ).roundToInt()
                            .coerceAtLeast(0)

                    } else {
                        0
                    }

                val drawY =
                    if (cropEnabled) {

                        (
                            cropCssY *
                                pixelsPerCssY
                        ).roundToInt()
                            .coerceAtLeast(0)

                    } else {
                        0
                    }

                val captureWidth =
                    if (
                        cropEnabled &&
                        cropCssWidth > 0f
                    ) {

                        (
                            cropCssWidth *
                                pixelsPerCssX
                        ).roundToInt()
                            .coerceAtLeast(1)

                    } else {

                        widgetWidth
                    }

                val captureHeight =
                    if (
                        cropEnabled &&
                        cropCssHeight > 0f
                    ) {

                        (
                            cropCssHeight *
                                pixelsPerCssY
                        ).roundToInt()
                            .coerceAtLeast(1)

                    } else {

                        widgetHeight
                    }

                /*
                 * Kun det valgte område oprettes
                 * som bitmap.
                 *
                 * Vi laver stadig IKKE et kæmpe
                 * bitmap af hele hjemmesiden.
                 */
                val selectedBitmap =
                    Bitmap.createBitmap(
                        captureWidth,
                        captureHeight,
                        Bitmap.Config.ARGB_8888
                    )

                val canvas =
                    Canvas(
                        selectedBitmap
                    )

                canvas.save()

                /*
                 * Ingen scrollTo().
                 *
                 * Hele dokumenttegningen flyttes,
                 * så det valgte CSS-område rammer
                 * bitmap'ets øverste venstre hjørne.
                 */
                canvas.translate(
                    -drawX.toFloat(),
                    -drawY.toFloat()
                )

                currentWeb.draw(
                    canvas
                )

                canvas.restore()

                /*
                 * Skaler udsnittet til widgettens
                 * faktiske størrelse.
                 */
                val finalBitmap =
                    if (
                        captureWidth != widgetWidth ||
                        captureHeight != widgetHeight
                    ) {

                        Bitmap.createScaledBitmap(
                            selectedBitmap,
                            widgetWidth,
                            widgetHeight,
                            true
                        )

                    } else {

                        selectedBitmap
                    }

                val views =
                    RemoteViews(
                        context.packageName,
                        R.layout.widget_layout
                    )

                views.setImageViewBitmap(
                    R.id.widget_image,
                    finalBitmap
                )

                manager.updateAppWidget(
                    appWidgetId,
                    views
                )

                /*
                 * Gem workerens beregnede værdier.
                 * De kan bruges til fejlsøgning,
                 * hvis vi stadig har en forskydning.
                 */
                prefs.edit()

                    .putInt(
                        "cropBitmapX_$appWidgetId",
                        drawX
                    )

                    .putInt(
                        "cropBitmapY_$appWidgetId",
                        drawY
                    )

                    .putFloat(
                        "cropWorkerCssViewportWidth_$appWidgetId",
                        workerCssViewportWidth
                    )

                    .putFloat(
                        "cropWorkerCssViewportHeight_$appWidgetId",
                        workerCssViewportHeight
                    )

                    .putFloat(
                        "cropWorkerPixelsPerCssX_$appWidgetId",
                        pixelsPerCssX
                    )

                    .putFloat(
                        "cropWorkerPixelsPerCssY_$appWidgetId",
                        pixelsPerCssY
                    )

                    .apply()

            } catch (_: Exception) {
            } finally {

                cleanup()
            }
        }

        /*
         * Spørg workerens hjemmeside om dens
         * faktiske CSS viewport.
         */
        fun readWorkerCssViewport() {

            val currentWeb =
                web
                    ?: run {
                        cleanup()
                        return
                    }

            val javascript =
                """
                (function() {
                    return [
                        window.innerWidth || document.documentElement.clientWidth || 1,
                        window.innerHeight || document.documentElement.clientHeight || 1
                    ].join(",");
                })();
                """.trimIndent()

            currentWeb.evaluateJavascript(
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

                    val cssWidth =
                        values.getOrNull(0)
                            ?.toFloatOrNull()
                            ?.coerceAtLeast(1f)
                            ?: renderWidth.toFloat()

                    val cssHeight =
                        values.getOrNull(1)
                            ?.toFloatOrNull()
                            ?.coerceAtLeast(1f)
                            ?: renderHeight.toFloat()

                    drawWidget(
                        cssWidth,
                        cssHeight
                    )

                } catch (_: Exception) {

                    /*
                     * Fallback:
                     * 1 CSS-pixel = 1 render-pixel.
                     */
                    drawWidget(
                        renderWidth.toFloat(),
                        renderHeight.toFloat()
                    )
                }
            }
        }

        return try {

            virtualDisplay =
                displayManager.createVirtualDisplay(
                    "WebWidget-$appWidgetId",
                    renderWidth,
                    renderHeight,
                    context.resources
                        .displayMetrics
                        .densityDpi,
                    surface,
                    DisplayManager
                        .VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                )

            val display =
                virtualDisplay?.display
                    ?: run {
                        cleanup()
                        return false
                    }

            presentation =
                Presentation(
                    context,
                    display
                )

            web =
                WebView(
                    presentation!!.context
                )

            web!!.setLayerType(
                View.LAYER_TYPE_SOFTWARE,
                null
            )

            web!!.settings.apply {

                javaScriptEnabled = true

                domStorageEnabled = true

                loadsImagesAutomatically = true

                useWideViewPort = true

                loadWithOverviewMode = false

                builtInZoomControls = false

                displayZoomControls = false

                offscreenPreRaster = true
            }

            presentation!!.setContentView(
                web
            )

            presentation!!.show()

            web!!.webViewClient =
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
                         * Behold 6 sekunders ventetid,
                         * så dynamisk indhold får tid
                         * til at falde på plads.
                         */
                        mainHandler.postDelayed({

                            readWorkerCssViewport()

                        }, 3000)
                    }
                }

            web!!.loadUrl(
                address
            )

            mainHandler.postDelayed({

                if (!completed) {
                    cleanup()
                }

            }, 25000)

            true

        } catch (_: Exception) {

            cleanup()
            false
        }
    }
}
