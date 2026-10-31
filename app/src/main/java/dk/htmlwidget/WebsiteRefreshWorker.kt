
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
import android.webkit.WebView
import android.webkit.WebResourceRequest
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

        val latch = CountDownLatch(1)

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
                25,
                TimeUnit.SECONDS
            )

        return if (completed && successful) {
            Result.success()
        } else {
            /*
             * Widgetten beholder sit gamle billede,
             * hvis hjemmesiden ikke kunne renderes.
             */
            Result.retry()
        }
    }

    private fun renderWebsite(
        appWidgetId: Int,
        address: String,
        latch: CountDownLatch
    ): Boolean {

        val context = applicationContext

        val manager =
            AppWidgetManager.getInstance(context)

        val options =
            manager.getAppWidgetOptions(appWidgetId)

        val density =
            context.resources
                .displayMetrics
                .density

        val width = (
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                300
            ) * density
        ).roundToInt().coerceAtLeast(1)

        val height = (
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                180
            ) * density
        ).roundToInt().coerceAtLeast(1)

        val displayManager =
            context.getSystemService(
                Context.DISPLAY_SERVICE
            ) as DisplayManager

        val surfaceTexture =
            SurfaceTexture(0)

        surfaceTexture.setDefaultBufferSize(
            width,
            height
        )

        val surface =
            Surface(surfaceTexture)

        var virtualDisplay: VirtualDisplay? = null
        var presentation: Presentation? = null
        var web: WebView? = null

        var completed = false

        fun cleanup() {

            if (completed) return

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

        return try {

            virtualDisplay =
                displayManager.createVirtualDisplay(
                    "WebWidget-$appWidgetId",
                    width,
                    height,
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

            web = WebView(
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
                offscreenPreRaster = true
            }

            presentation!!.setContentView(web)

            presentation!!.show()

            web!!.webViewClient =
                object : WebViewClient() {

                    override fun onPageFinished(
                        view: WebView?,
                        url: String?
                    ) {

                        mainHandler.postDelayed({

                            if (completed) {
                                return@postDelayed
                            }

                            try {

                                val bitmap =
                                    Bitmap.createBitmap(
                                        width,
                                        height,
                                        Bitmap.Config.ARGB_8888
                                    )

                                val canvas =
                                    Canvas(bitmap)

                                web!!.draw(canvas)

                                val views =
                                    RemoteViews(
                                        context.packageName,
                                        R.layout.widget_layout
                                    )

                                views.setImageViewBitmap(
                                    R.id.widget_image,
                                    bitmap
                                )

                                manager.updateAppWidget(
                                    appWidgetId,
                                    views
                                )

                            } catch (_: Exception) {
                            } finally {
                                cleanup()
                            }

                        }, 5000)
                    }
                }

            web!!.loadUrl(address)

            /*
             * Sikkerheds-timeout.
             */
            mainHandler.postDelayed({
                cleanup()
            }, 20000)

            true

        } catch (_: Exception) {

            cleanup()
            false
        }
    }
}
