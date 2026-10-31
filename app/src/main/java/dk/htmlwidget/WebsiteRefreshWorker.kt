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
         * Dokumentkoordinater fra område-vælgeren.
         *
         * Disse er gemt i WebViewens
         * dokumentkoordinater.
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

        /*
         * Scale fra WebView'en, hvor brugeren
         * valgte området.
         */
        val savedWebScale =
            prefs.getFloat(
                "cropWebScale_$appWidgetId",
                1f
            ).coerceAtLeast(0.01f)

        /*
         * Samme viewport som område-vælgeren.
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

        /*
         * Størrelsen på det valgte område
         * i dokumentkoordinater.
         */
        val documentSelectionWidth =
            prefs.getInt(
                "cropDocumentSelectionWidth_$appWidgetId",
                0
            )

        val documentSelectionHeight =
            prefs.getInt(
                "cropDocumentSelectionHeight_$appWidgetId",
                0
            )

        /*
         * Almindelig pixelstørrelse som fallback.
         */
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
         * Samme viewportstørrelse som
         * CropWebsiteActivity.
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
                 * Workerens faktiske WebView-scale.
                 *
                 * Selv med samme viewport kan vi ikke
                 * bare antage, at den er identisk.
                 */
                val workerWebScale =
                    currentWeb.scale
                        .coerceAtLeast(0.01f)

                /*
                 * Dokumentkoordinaterne skal nu
                 * oversættes til workerens
                 * tegningskoordinater.
                 *
                 * Eksempel:
                 *
                 * Valgt dokument-Y = 2000
                 * Worker scale = 2
                 *
                 * Tegnings-Y = 4000 px.
                 */
                val drawX =
                    if (cropEnabled) {

                        (
                            cropDocumentX *
                                workerWebScale
                        ).roundToInt()
                            .coerceAtLeast(0)

                    } else {
                        0
                    }

                val drawY =
                    if (cropEnabled) {

                        (
                            cropDocumentY *
                                workerWebScale
                        ).roundToInt()
                            .coerceAtLeast(0)

                    } else {
                        0
                    }

                /*
                 * Områdets størrelse oversættes
                 * på samme måde.
                 */
                val captureWidth =
                    if (
                        cropEnabled &&
                        documentSelectionWidth > 0
                    ) {

                        (
                            documentSelectionWidth *
                                workerWebScale
                        ).roundToInt()
                            .coerceAtLeast(1)

                    } else if (
                        cropEnabled &&
                        savedSelectionWidth > 0
                    ) {

                        /*
                         * Hvis de nye dokumentmål
                         * mangler, korrigerer vi den
                         * gamle størrelse mellem de
                         * to scales.
                         */
                        (
                            savedSelectionWidth *
                                (
                                    workerWebScale /
                                        savedWebScale
                                )
                        ).roundToInt()
                            .coerceAtLeast(1)

                    } else {

                        widgetWidth
                    }

                val captureHeight =
                    if (
                        cropEnabled &&
                        documentSelectionHeight > 0
                    ) {

                        (
                            documentSelectionHeight *
                                workerWebScale
                        ).roundToInt()
                            .coerceAtLeast(1)

                    } else if (
                        cropEnabled &&
                        savedSelectionHeight > 0
                    ) {

                        (
                            savedSelectionHeight *
                                (
                                    workerWebScale /
                                        savedWebScale
                                )
                        ).roundToInt()
                            .coerceAtLeast(1)

                    } else {

                        widgetHeight
                    }

                /*
                 * Kun det valgte område oprettes
                 * som bitmap.
                 *
                 * Stadig intet kæmpe helside-bitmap.
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
                 * Flyt Canvas til den præcise
                 * dokumentposition.
                 *
                 * WebView'en bliver IKKE scrollet.
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
                 * Det valgte område skaleres til
                 * widgettens faktiske størrelse.
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
                 * Gem de faktiske koordinater,
                 * så vi har dem til fejlsøgning.
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
                        "cropWorkerWebScale_$appWidgetId",
                        workerWebScale
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
             * Samme indstillinger som
             * område-vælgeren.
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
