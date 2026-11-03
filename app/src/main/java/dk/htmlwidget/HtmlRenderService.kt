package dk.htmlwidget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class HtmlRenderService : Service() {

    private val handler =
        Handler(Looper.getMainLooper())

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val appWidgetId =
            intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        handler.post {

            render(
                appWidgetId
            )
        }

        return START_NOT_STICKY
    }

    private fun render(
        appWidgetId: Int
    ) {

        val prefs =
            getSharedPreferences(
                "settings",
                MODE_PRIVATE
            )

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {

            stopSelf()

            return
        }

        val localUri =
            prefs.getString(
                "localUri_$appWidgetId",
                null
            )

        /*
         * Denne service håndterer
         * KUN lokal HTML.
         */
        if (
            localUri.isNullOrBlank()
        ) {

            stopSelf()

            return
        }

        val html =
            try {

                contentResolver
                    .openInputStream(
                        Uri.parse(localUri)
                    )
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }

            } catch (_: Exception) {

                null
            }

        if (
            html.isNullOrBlank()
        ) {

            stopSelf()

            return
        }

        val manager =
            AppWidgetManager.getInstance(
                this
            )

        val ids =
            intArrayOf(
                appWidgetId
            )

        val options =
            manager.getAppWidgetOptions(
                appWidgetId
            )

        val density =
            resources
                .displayMetrics
                .density

        val width =
            (
                options.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                    300
                ) * density
                )
                .roundToInt()
                .coerceAtLeast(1)

        val height =
            (
                options.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                    180
                ) * density
                )
                .roundToInt()
                .coerceAtLeast(1)

        val web =
            WebView(this)

        web.setLayerType(
            View.LAYER_TYPE_SOFTWARE,
            null
        )

        web.settings.apply {

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
        }

        web.measure(

            View.MeasureSpec.makeMeasureSpec(
                width,
                View.MeasureSpec.EXACTLY
            ),

            View.MeasureSpec.makeMeasureSpec(
                height,
                View.MeasureSpec.EXACTLY
            )
        )

        web.layout(
            0,
            0,
            width,
            height
        )

        web.webViewClient =
            object :
                WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    handler.postDelayed({

                        web.evaluateJavascript(
                            """
                            window.dispatchEvent(
                                new Event('resize')
                            );
                            """.trimIndent()
                        ) {

                            handler.postDelayed({

                                makeBitmap(
                                    web,
                                    manager,
                                    ids,
                                    width,
                                    height
                                )

                            }, 1200)
                        }

                    }, 4000)
                }
            }

        web.loadDataWithBaseURL(
            localUri,
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun makeBitmap(
        web: WebView,
        manager: AppWidgetManager,
        ids: IntArray,
        width: Int,
        height: Int
    ) {

        web.invalidate()

        handler.postDelayed({

            val bitmap =
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
                )

            val canvas =
                Canvas(
                    bitmap
                )

            web.draw(
                canvas
            )

            ids.forEach { id ->

                /*
                 * Opdater selve widget-billedet.
                 */
                val views =
                    RemoteViews(
                        packageName,
                        R.layout.widget_layout
                    )

                views.setImageViewBitmap(
                    R.id.widget_image,
                    bitmap
                )

                manager.updateAppWidget(
                    id,
                    views
                )

                /*
                 * --------------------------------
                 * OPDATERINGSSTATUS
                 * --------------------------------
                 *
                 * Den almindelige updateAppWidget
                 * ovenfor nulstiller statusfeltet.
                 *
                 * Derfor sætter vi status bagefter.
                 */
                val prefs =
                    getSharedPreferences(
                        "settings",
                        MODE_PRIVATE
                    )

                val showStatus =
                    prefs.getBoolean(
                        "showUpdateStatus_$id",
                        true
                    )

                /*
                 * Gem tidspunktet for den
                 * vellykkede lokale rendering.
                 */
                prefs.edit()
                    .putLong(
                        "lastSuccessfulUpdate_$id",
                        System.currentTimeMillis()
                    )
                    .apply()

                if (showStatus) {

                    val time =
                        SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                        ).format(
                            Date()
                        )

                    val language =
                        prefs.getString(
                            "appLanguage",
                            "da"
                        ) ?: "da"

                    val configuration =
                        android.content.res.Configuration(
                            resources.configuration
                        )

                    configuration.setLocale(
                        Locale(language)
                    )

                    val localizedContext =
                        createConfigurationContext(
                            configuration
                        )

                    val statusViews =
                        RemoteViews(
                            packageName,
                            R.layout.widget_layout
                        )

                    statusViews.setTextViewText(
                        R.id.widget_status,
                        localizedContext.getString(
                            R.string.status_updated,
                            time
                        )
                    )

                    statusViews.setViewVisibility(
                        R.id.widget_status,
                        View.VISIBLE
                    )

                    manager.partiallyUpdateAppWidget(
                        id,
                        statusViews
                    )
                }
            }

            web.destroy()

            stopSelf()

        }, 300)
    }

    companion object {

        fun start(
            context: Context,
            appWidgetId: Int
        ) {

            val intent =
                Intent(
                    context,
                    HtmlRenderService::class.java
                ).apply {

                    putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        appWidgetId
                    )
                }

            context.startService(
                intent
            )
        }
    }
}
