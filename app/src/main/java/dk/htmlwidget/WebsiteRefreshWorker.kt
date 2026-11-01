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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
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

        val manualRefresh =
            inputData.getBoolean(
                "manualRefresh",
                false
            )

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

            if (
                manualRefresh
            ) {

                showFailureStatus(
                    appWidgetId
                )
            }

            return Result.success()
        }

        val latch =
            CountDownLatch(1)

        var successful =
            false

        mainHandler.post {

            successful =
                renderWebsite(
                    appWidgetId,
                    address,
                    latch,
                    manualRefresh
                )
        }

        val completed =
            latch.await(
                30,
                TimeUnit.SECONDS
            )

        if (
            !completed
        ) {

            if (
                manualRefresh
            ) {

                mainHandler.post {

                    showFailureStatus(
                        appWidgetId
                    )
                }
            }

            return Result.retry()
        }

        return if (
            successful
        ) {

            Result.success()

        } else {

            if (
                manualRefresh
            ) {

                mainHandler.post {

                    showFailureStatus(
                        appWidgetId
                    )
                }
            }

            Result.retry()
        }
    }

    /*
     * ----------------------------------------
     * FEJLSTATUS
     * ----------------------------------------
     *
     * Ændrer KUN statusfeltet.
     *
     * Det sidste fungerende widgetbillede
     * bliver stående.
     */
    private fun showFailureStatus(
        appWidgetId: Int
    ) {

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            return
        }

        val context =
            applicationContext

        val prefs =
            context.getSharedPreferences(
                "settings",
                Context.MODE_PRIVATE
            )

        val showStatus =
            prefs.getBoolean(
                "showUpdateStatus_$appWidgetId",
                true
            )

        if (
            !showStatus
        ) {
            return
        }

        val manager =
            AppWidgetManager.getInstance(
                context
            )

        val views =
            RemoteViews(
                context.packageName,
                R.layout.widget_layout
            )

        views.setTextViewText(
            R.id.widget_status,
            "Kunne ikke opdatere"
        )

        views.setViewVisibility(
            R.id.widget_status,
            View.VISIBLE
        )

        manager.partiallyUpdateAppWidget(
            appWidgetId,
            views
        )
    }

    private fun renderWebsite(
        appWidgetId: Int,
        address: String,
        latch: CountDownLatch,
        manualRefresh: Boolean
    ): Boolean {

        val context =
            applicationContext

        val prefs =
            context.getSharedPreferences(
                "settings",
                Context.MODE_PRIVATE
            )

        /*
         * ----------------------------------------
         * AUTOMATISK SKIFT MELLEM VISNINGER
         * ----------------------------------------
         *
         * Visning 1 bruger fortsat de gamle
         * crop-nøgler.
         *
         * Det betyder, at eksisterende widgets
         * fortsætter med at virke som før.
         */
        val autoSwitchViews =
            prefs.getBoolean(
                "autoSwitchViews_$appWidgetId",
                false
            )

        val cropViewCount =
            prefs.getInt(
                "cropViewCount_$appWidgetId",
                0
            ).coerceAtLeast(0)

        val lastDisplayedView =
            prefs.getInt(
                "currentCropView_$appWidgetId",
                0
            )

        val selectedView =
            if (
                autoSwitchViews &&
                cropViewCount > 1
            ) {

                if (
                    lastDisplayedView < 1 ||
                    lastDisplayedView >= cropViewCount
                ) {

                    1

                } else {

                    lastDisplayedView + 1
                }

            } else {

                1
            }

        /*
         * Visning 1 læses fra de oprindelige
         * nøgler.
         *
         * Visning 2, 3, 4 osv. læses fra de
         * nye nummererede nøgler.
         */
        val cropEnabled =
            if (
                selectedView == 1
            ) {

                prefs.getBoolean(
                    "cropEnabled_$appWidgetId",
                    false
                )

            } else {

                prefs.getBoolean(
                    "cropView_${selectedView}_enabled_$appWidgetId",
                    false
                )
            }

        val cropCssX =
            if (
                selectedView == 1
            ) {

                prefs.getFloat(
                    "cropCssX_$appWidgetId",
                    0f
                )

            } else {

                prefs.getFloat(
                    "cropView_${selectedView}_cropCssX_$appWidgetId",
                    0f
                )
            }.coerceAtLeast(0f)

        val cropCssY =
            if (
                selectedView == 1
            ) {

                prefs.getFloat(
                    "cropCssY_$appWidgetId",
                    0f
                )

            } else {

                prefs.getFloat(
                    "cropView_${selectedView}_cropCssY_$appWidgetId",
                    0f
                )
            }.coerceAtLeast(0f)

        val cropCssWidth =
            if (
                selectedView == 1
            ) {

                prefs.getFloat(
                    "cropCssWidth_$appWidgetId",
                    0f
                )

            } else {

                prefs.getFloat(
                    "cropView_${selectedView}_cropCssWidth_$appWidgetId",
                    0f
                )
            }.coerceAtLeast(0f)

        val cropCssHeight =
            if (
                selectedView == 1
            ) {

                prefs.getFloat(
                    "cropCssHeight_$appWidgetId",
                    0f
                )

            } else {

                prefs.getFloat(
                    "cropView_${selectedView}_cropCssHeight_$appWidgetId",
                    0f
                )
            }.coerceAtLeast(0f)

        val savedViewportWidth =
            if (
                selectedView == 1
            ) {

                prefs.getInt(
                    "cropViewportWidth_$appWidgetId",
                    0
                )

            } else {

                prefs.getInt(
                    "cropView_${selectedView}_cropViewportWidth_$appWidgetId",
                    0
                )
            }

        val savedViewportHeight =
            if (
                selectedView == 1
            ) {

                prefs.getInt(
                    "cropViewportHeight_$appWidgetId",
                    0
                )

            } else {

                prefs.getInt(
                    "cropView_${selectedView}_cropViewportHeight_$appWidgetId",
                    0
                )
            }

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
                )
                .roundToInt()
                .coerceAtLeast(1)

        val widgetHeight =
            (
                options.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                    180
                ) * density
                )
                .roundToInt()
                .coerceAtLeast(1)

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
            Surface(
                surfaceTexture
            )

        var virtualDisplay: VirtualDisplay? =
            null

        var presentation: Presentation? =
            null

        var web: WebView? =
            null

        var completed =
            false

        /*
         * Denne værdi fortæller cleanup(),
         * om vi rent faktisk nåede frem til
         * et færdigt og gyldigt widgetbillede.
         */
        var renderSucceeded =
            false

        fun cleanup() {

            if (
                completed
            ) {
                return
            }

            completed =
                true

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

            /*
             * Manuel fejl:
             *
             * Vi ændrer kun status.
             * Det gamle billede bliver stående.
             */
            if (
                manualRefresh &&
                !renderSucceeded
            ) {

                showFailureStatus(
                    appWidgetId
                )
            }

            latch.countDown()
        }

        fun showFinishedStatus() {

            if (
                !manualRefresh
            ) {
                return
            }

            val showStatus =
                prefs.getBoolean(
                    "showUpdateStatus_$appWidgetId",
                    true
                )

            if (
                !showStatus
            ) {
                return
            }

            val time =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                ).format(
                    Date()
                )

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.widget_layout
                )

            views.setTextViewText(
                R.id.widget_status,
                "Opdateret $time"
            )

            views.setViewVisibility(
                R.id.widget_status,
                View.VISIBLE
            )

            manager.partiallyUpdateAppWidget(
                appWidgetId,
                views
            )
        }

        /*
         * Beregn checksum af det færdige billede.
         */
        fun bitmapChecksum(
            bitmap: Bitmap
        ): Long {

            val crc =
                CRC32()

            val row =
                IntArray(
                    bitmap.width
                )

            for (
                y in 0 until bitmap.height
            ) {

                bitmap.getPixels(
                    row,
                    0,
                    bitmap.width,
                    0,
                    y,
                    bitmap.width,
                    1
                )

                for (
                    pixel in row
                ) {

                    crc.update(
                        pixel ushr 24 and 0xFF
                    )

                    crc.update(
                        pixel ushr 16 and 0xFF
                    )

                    crc.update(
                        pixel ushr 8 and 0xFF
                    )

                    crc.update(
                        pixel and 0xFF
                    )
                }
            }

            return crc.value
        }
                fun drawWidget(
            workerCssViewportWidth: Float,
            workerCssViewportHeight: Float
        ) {

            if (
                completed
            ) {
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
                 * --------------------------------
                 * FUNGERENDE CSS-MAPPING
                 * --------------------------------
                 *
                 * ÆNDRES IKKE.
                 */
                val pixelsPerCssX =
                    renderWidth.toFloat() /
                        workerCssViewportWidth
                            .coerceAtLeast(1f)

                val pixelsPerCssY =
                    renderHeight.toFloat() /
                        workerCssViewportHeight
                            .coerceAtLeast(1f)

                val drawX =
                    if (
                        cropEnabled
                    ) {

                        (
                            cropCssX *
                                pixelsPerCssX
                            )
                            .roundToInt()
                            .coerceAtLeast(0)

                    } else {

                        0
                    }

                val drawY =
                    if (
                        cropEnabled
                    ) {

                        (
                            cropCssY *
                                pixelsPerCssY
                            )
                            .roundToInt()
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
                            )
                            .roundToInt()
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
                            )
                            .roundToInt()
                            .coerceAtLeast(1)

                    } else {

                        widgetHeight
                    }

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

                canvas.translate(
                    -drawX.toFloat(),
                    -drawY.toFloat()
                )

                currentWeb.draw(
                    canvas
                )

                canvas.restore()

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

/*
 * ----------------------------------------
 * GEM PREVIEW-BILLEDE
 * ----------------------------------------
 *
 * Gemmer det samme færdige bitmap,
 * som bliver vist på widgetten.
 */
try {

    val previewFile =
        File(
            context.filesDir,
            "widget_$appWidgetId.png"
        )

    FileOutputStream(
        previewFile
    ).use { output ->

        finalBitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            output
        )
    }

} catch (_: Exception) {
    /*
     * Preview må aldrig få selve
     * widget-opdateringen til at fejle.
     */
}
                    
                /*
                 * ----------------------------------------
                 * CHECKSUM
                 * ----------------------------------------
                 */
                val newChecksum =
                    bitmapChecksum(
                        finalBitmap
                    )

                val oldChecksum =
                    prefs.getLong(
                        "lastBitmapChecksum_$appWidgetId",
                        -1L
                    )

                val imageChanged =
                    newChecksum !=
                        oldChecksum

                /*
                 * Send kun nyt billede til Android,
                 * hvis indholdet har ændret sig.
                 */
                if (
                    imageChanged
                ) {

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
                }

                /*
                 * ----------------------------------------
                 * RENDERING LYKKEDES
                 * ----------------------------------------
                 */
                renderSucceeded =
                    true

                val successfulUpdateTime =
                    System.currentTimeMillis()

                val editor =
                    prefs.edit()

                        .putLong(
                            "lastBitmapChecksum_$appWidgetId",
                            newChecksum
                        )

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

                        .putLong(
                            "lastSuccessfulUpdate_$appWidgetId",
                            successfulUpdateTime
                        )

                /*
                 * ----------------------------------------
                 * GEM AKTUEL VISNING
                 * ----------------------------------------
                 *
                 * VIGTIGT:
                 *
                 * Vi gemmer først selectedView EFTER
                 * en vellykket rendering.
                 *
                 * Hvis renderingen fejler, springer
                 * widgetten derfor ikke en visning over.
                 */
                if (
                    autoSwitchViews &&
                    cropViewCount > 1
                ) {

                    editor.putInt(
                        "currentCropView_$appWidgetId",
                        selectedView
                    )

                } else {

                    editor.putInt(
                        "currentCropView_$appWidgetId",
                        1
                    )
                }

                editor.apply()

                showFinishedStatus()

                /*
                 * Frigiv midlertidig bitmap.
                 */
                if (
                    finalBitmap !==
                    selectedBitmap
                ) {

                    try {

                        selectedBitmap.recycle()

                    } catch (_: Exception) {
                    }
                }

            } catch (_: Exception) {

                /*
                 * FEJL:
                 *
                 * Ingen updateAppWidget.
                 * Ingen ny checksum.
                 * Intet nyt success-tidspunkt.
                 * Ingen ændring af currentCropView.
                 *
                 * Det sidste gode billede
                 * bliver stående.
                 */

            } finally {

                cleanup()
            }
        }

        /*
         * ----------------------------------------
         * COOKIE-BANNERE
         * ----------------------------------------
         */
        fun hideCookieBanners(
            after: () -> Unit
        ) {

            val currentWeb =
                web
                    ?: run {

                        cleanup()
                        return
                    }

            val hideCookieBanners =
                prefs.getBoolean(
                    "hideCookieBanners_$appWidgetId",
                    prefs.getBoolean(
                        "hideCookieBanners",
                        true
                    )
                )

            if (
                !hideCookieBanners
            ) {

                after()
                return
            }

            val javascript =
                """
                (function() {
                    try {

                        const selectors = [
                            '#onetrust-banner-sdk',
                            '#onetrust-consent-sdk',
                            '.qc-cmp2-container',
                            '.fc-consent-root',
                            '#CybotCookiebotDialog',
                            '#CookiebotWidget',
                            '[id^="sp_message_container"]',
                            '[class*="sp_message_container"]'
                        ];

                        selectors.forEach(function(selector) {

                            document
                                .querySelectorAll(selector)
                                .forEach(function(element) {

                                    element.remove();

                                });
                        });

                        document.documentElement.style.setProperty(
                            'overflow',
                            'auto',
                            'important'
                        );

                        if (document.body) {

                            document.body.style.setProperty(
                                'overflow',
                                'auto',
                                'important'
                            );
                        }

                    } catch (e) {
                    }

                    return 'ok';

                })();
                """.trimIndent()

            currentWeb.evaluateJavascript(
                javascript
            ) {

                after()
            }
        }

        /*
         * ----------------------------------------
         * CSS VIEWPORT
         * ----------------------------------------
         *
         * Vores fungerende crop-metode.
         * ÆNDRES IKKE.
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
                            .replace(
                                "\\\"",
                                "\""
                            )

                    val values =
                        cleaned.split(",")

                    val cssWidth =
                        values
                            .getOrNull(0)
                            ?.toFloatOrNull()
                            ?.coerceAtLeast(1f)
                            ?: renderWidth
                                .toFloat()

                    val cssHeight =
                        values
                            .getOrNull(1)
                            ?.toFloatOrNull()
                            ?.coerceAtLeast(1f)
                            ?: renderHeight
                                .toFloat()

                    drawWidget(
                        cssWidth,
                        cssHeight
                    )

                } catch (_: Exception) {

                    drawWidget(
                        renderWidth.toFloat(),
                        renderHeight.toFloat()
                    )
                }
            }
        }

        /*
         * ----------------------------------------
         * OPRET SKJULT WEBVIEW
         * ----------------------------------------
         */
        return try {

            virtualDisplay =
                displayManager
                    .createVirtualDisplay(
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

                offscreenPreRaster =
                    true
            }

            presentation!!
                .setContentView(
                    web
                )

            presentation!!
                .show()

            web!!.webViewClient =
                object :
                    WebViewClient() {

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {

                        val adBlockEnabled =
                            prefs.getBoolean(
                                "adblock",
                                true
                            )

                        if (
                            !adBlockEnabled
                        ) {

                            return super
                                .shouldInterceptRequest(
                                    view,
                                    request
                                )
                        }

                        val host =
                            request?.url
                                ?.host
                                ?.lowercase()
                                ?: return super
                                    .shouldInterceptRequest(
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

                        return if (
                            blocked
                        ) {

                            android.webkit.WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                null
                            )

                        } else {

                            super
                                .shouldInterceptRequest(
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
                         * VIGTIGT:
                         *
                         * Beholder præcis vores
                         * fungerende ventetid:
                         *
                         * 2000 ms
                         */
                        mainHandler.postDelayed({

                            hideCookieBanners {

                                readWorkerCssViewport()
                            }

                        }, 2000)
                    }
                }

            web!!.loadUrl(
                address
            )

            /*
             * Sikkerhedstimeout.
             *
             * Hvis vi når hertil uden et
             * færdigt billede, vil cleanup()
             * vise fejlstatus ved manuel refresh.
             */
            mainHandler.postDelayed({

                if (
                    !completed
                ) {

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
