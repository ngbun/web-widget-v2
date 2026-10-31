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
                35,
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
         * De relative koordinater fra
         * den levende område-vælger.
         */
        val relativeX =
            prefs.getFloat(
                "cropRelativeX_$appWidgetId",
                0f
            ).coerceIn(
                0f,
                1f
            )

        val relativeY =
            prefs.getFloat(
                "cropRelativeY_$appWidgetId",
                0f
            ).coerceIn(
                0f,
                1f
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

        val width =
            (
                options.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                    300
                ) * density
                ).roundToInt()
                .coerceAtLeast(1)

        val height =
            (
                options.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                    180
                ) * density
                ).roundToInt()
                .coerceAtLeast(1)

        /*
         * VIGTIGT:
         *
         * Ikke længere 12 × widgethøjden.
         *
         * WebView'en har nu en almindelig viewport.
         * Vi scroller selve dokumentet til det
         * valgte område.
         */
        val renderWidth =
            (width * 3)
                .coerceAtLeast(width)

        val renderHeight =
            (height * 3)
                .coerceAtLeast(height)

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

        var completed =
            false

        fun cleanup() {

            if (completed) {
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

            latch.countDown()
        }

        /*
         * Tag screenshot af den AKTUELLE viewport.
         *
         * WebView'en er allerede scrollet til
         * det valgte område på dette tidspunkt.
         */
        fun captureWidget() {

            if (completed) {
                return
            }

            try {

                val viewportBitmap =
                    Bitmap.createBitmap(
                        renderWidth,
                        renderHeight,
                        Bitmap.Config.ARGB_8888
                    )

                val canvas =
                    Canvas(
                        viewportBitmap
                    )

                web?.draw(
                    canvas
                )

                /*
                 * X-positionen inden for den
                 * aktuelle viewport.
                 *
                 * Lodret position håndteres ved
                 * at scrolle WebView'en.
                 */
                val maxCropX =
                    (renderWidth - width)
                        .coerceAtLeast(0)

                val cropX =
                    (
                        relativeX *
                            renderWidth
                        ).roundToInt()
                        .coerceIn(
                            0,
                            maxCropX
                        )

                /*
                 * Efter scroll ligger det valgte
                 * område øverst i WebView'en.
                 */
                val cropY =
                    0

                val finalBitmap =
                    Bitmap.createBitmap(
                        viewportBitmap,
                        cropX,
                        cropY,
                        width,
                        height
                    )

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

            } catch (_: Exception) {
            } finally {

                cleanup()
            }
        }

        /*
         * Find sidens højde og scroll til
         * den gemte relative Y-position.
         */
        fun moveToSelectedArea() {

            val currentWeb =
                web
                    ?: run {
                        cleanup()
                        return
                    }

            if (!cropEnabled) {

                mainHandler.postDelayed({

                    captureWidget()

                }, 1500)

                return
            }

            /*
             * Vi spørger den færdige hjemmeside
             * om dens faktiske scrollbare højde.
             *
             * Derefter scrolles til samme relative
             * sted som brugeren valgte.
             */
            val script =
                """
                (function() {
                    var doc = document.documentElement;
                    var body = document.body;

                    var fullHeight = Math.max(
                        body ? body.scrollHeight : 0,
                        body ? body.offsetHeight : 0,
                        doc ? doc.clientHeight : 0,
                        doc ? doc.scrollHeight : 0,
                        doc ? doc.offsetHeight : 0
                    );

                    var viewportHeight =
                        window.innerHeight || 1;

                    var maxScroll =
                        Math.max(
                            0,
                            fullHeight - viewportHeight
                        );

                    var wanted =
                        Math.round(
                            maxScroll * $relativeY
                        );

                    window.scrollTo(
                        0,
                        wanted
                    );

                    return JSON.stringify({
                        fullHeight: fullHeight,
                        viewportHeight: viewportHeight,
                        maxScroll: maxScroll,
                        wanted: wanted,
                        actual: window.scrollY
                    });
                })();
                """.trimIndent()

            currentWeb.evaluateJavascript(
                script
            ) {
                /*
                 * WebView/Chromium skal have tid
                 * til at tegne den nye position.
                 *
                 * Vi tager IKKE screenshot med det
                 * samme efter scroll.
                 */
                mainHandler.postDelayed({

                    /*
                     * Scroll endnu en gang til den
                     * samme relative position.
                     *
                     * Dette hjælper sider, der ændrer
                     * højde efter lazy-loading.
                     */
                    val secondScript =
                        """
                        (function() {
                            var doc = document.documentElement;
                            var body = document.body;

                            var fullHeight = Math.max(
                                body ? body.scrollHeight : 0,
                                body ? body.offsetHeight : 0,
                                doc ? doc.clientHeight : 0,
                                doc ? doc.scrollHeight : 0,
                                doc ? doc.offsetHeight : 0
                            );

                            var viewportHeight =
                                window.innerHeight || 1;

                            var maxScroll =
                                Math.max(
                                    0,
                                    fullHeight - viewportHeight
                                );

                            var wanted =
                                Math.round(
                                    maxScroll * $relativeY
                                );

                            window.scrollTo(
                                0,
                                wanted
                            );

                            return window.scrollY;
                        })();
                        """.trimIndent()

                    currentWeb.evaluateJavascript(
                        secondScript
                    ) {

                        /*
                         * Vent på at den scrollede
                         * viewport er tegnet.
                         */
                        mainHandler.postDelayed({

                            captureWidget()

                        }, 2500)
                    }

                }, 1500)
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

                offscreenPreRaster =
                    true
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
                            context.getSharedPreferences(
                                "settings",
                                Context.MODE_PRIVATE
                            ).getBoolean(
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
                         * Først lader vi hele siden,
                         * billeder og JavaScript falde
                         * på plads.
                         */
                        mainHandler.postDelayed({

                            moveToSelectedArea()

                        }, 5000)
                    }
                }

            web!!.loadUrl(
                address
            )

            /*
             * Sikkerheds-timeout.
             */
            mainHandler.postDelayed({

                if (!completed) {
                    cleanup()
                }

            }, 30000)

            true

        } catch (_: Exception) {

            cleanup()
            false
        }
    }
}
