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
         * NYE præcise værdier fra
         * CropWebsiteActivity.
         */
        val cropDocumentX =
            prefs.getInt(
                "cropDocumentX_$appWidgetId",
                0
            ).coerceAtLeast(0)

        val cropDocumentY =
            prefs.getInt(
                "cropDocumentY_$appWidgetId",
                0
            ).coerceAtLeast(0)

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

        val savedSelectionWidth =
            prefs.getInt(
                "cropSelectionWidth_$appWidgetId",
                0
            )

        val savedSelectionHeight =
            prefs.getInt(
                "cropSelectionHeight_$appWidgetId",
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

        /*
         * Widgettens faktiske outputstørrelse.
         */
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
         * KRITISK ÆNDRING:
         *
         * Hvis der er gemt et område, bruger worker'en
         * præcis samme viewportbredde som WebView'en
         * i CropWebsiteActivity.
         *
         * Dermed bør responsive hjemmesider få
         * samme layout begge steder.
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

        fun drawWidget() {

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
                 * Områdets størrelse.
                 *
                 * Ved crop bruger vi præcis samme
                 * størrelse som den røde ramme havde.
                 */
                val selectionWidth =
                    if (
                        cropEnabled &&
                        savedSelectionWidth > 0
                    ) {
                        savedSelectionWidth
                    } else {
                        widgetWidth
                    }.coerceAtLeast(1)

                val selectionHeight =
                    if (
                        cropEnabled &&
                        savedSelectionHeight > 0
                    ) {
                        savedSelectionHeight
                    } else {
                        widgetHeight
                    }.coerceAtLeast(1)

                /*
                 * Vi laver KUN et bitmap på størrelse
                 * med det valgte område.
                 *
                 * Ikke hele hjemmesiden.
                 */
                val selectedBitmap =
                    Bitmap.createBitmap(
                        selectionWidth,
                        selectionHeight,
                        Bitmap.Config.ARGB_8888
                    )

                val selectedCanvas =
                    Canvas(
                        selectedBitmap
                    )

                selectedCanvas.save()

                /*
                 * Ingen WebView-scroll.
                 *
                 * Vi flytter kun Canvas'en, så
                 * dokumentets valgte koordinat
                 * lander ved 0,0 i bitmap'et.
                 */
                if (cropEnabled) {

                    selectedCanvas.translate(
                        -cropDocumentX.toFloat(),
                        -cropDocumentY.toFloat()
                    )
                }

                currentWeb.draw(
                    selectedCanvas
                )

                selectedCanvas.restore()

                /*
                 * Den røde ramme har samme form som
                 * widgetten, men dens pixelstørrelse
                 * er normalt mindre.
                 *
                 * Derfor skaleres det valgte udsnit
                 * til widgettens faktiske størrelse.
                 */
                val finalBitmap =
                    if (
                        selectionWidth != widgetWidth ||
                        selectionHeight != widgetHeight
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
                 * Behold koordinaterne også i de
                 * gamle felter til fejlsøgning/
                 * kompatibilitet.
                 */
                prefs.edit()
                    .putInt(
                        "cropBitmapX_$appWidgetId",
                        cropDocumentX
                    )
                    .putInt(
                        "cropBitmapY_$appWidgetId",
                        cropDocumentY
                    )
                    .apply()

            } catch (_: Exception) {
            } finally {

                cleanup()
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

            /*
             * Samme WebView-indstillinger som
             * CropWebsiteActivity.
             */
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
                         * Behold den stabile ventetid.
                         */
                        mainHandler.postDelayed({

                            drawWidget()

                        }, 6000)
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
