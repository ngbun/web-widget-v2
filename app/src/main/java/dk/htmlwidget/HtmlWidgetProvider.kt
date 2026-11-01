package dk.htmlwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class HtmlWidgetProvider :
    AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        refresh(
            context,
            appWidgetIds
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {

        super.onAppWidgetOptionsChanged(
            context,
            appWidgetManager,
            appWidgetId,
            newOptions
        )

        refresh(
            context,
            intArrayOf(
                appWidgetId
            )
        )
    }

    override fun onEnabled(
        context: Context
    ) {

        super.onEnabled(
            context
        )
    }

    /*
     * ----------------------------------------
     * TRYK PÅ WIDGET
     * ----------------------------------------
     *
     * 1 tryk:
     * Opdater widgetten.
     *
     * 2 tryk inden for 800 ms:
     * Åbn indstillingerne for præcis
     * denne widget.
     */
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        super.onReceive(
            context,
            intent
        )

        if (
            intent.action ==
            ACTION_REFRESH_NOW
        ) {

            val appWidgetId =
                intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )

            if (
                appWidgetId ==
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) {
                return
            }

            val prefs =
                context.getSharedPreferences(
                    "settings",
                    Context.MODE_PRIVATE
                )

            val now =
                System.currentTimeMillis()

            val lastClickTime =
                prefs.getLong(
                    "lastClickTime_$appWidgetId",
                    0L
                )

            val isDoubleClick =
                now - lastClickTime < 800L

            if (
                isDoubleClick
            ) {

                /*
                 * Dobbelttryk.
                 *
                 * Fjern klik-tidspunktet, så næste
                 * almindelige tryk starter på ny.
                 */
                prefs.edit()
                    .remove(
                        "lastClickTime_$appWidgetId"
                    )
                    .apply()

                val setupIntent =
                    Intent(
                        context,
                        SetupActivity::class.java
                    ).apply {

                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK

                        putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId
                        )
                    }

                context.startActivity(
                    setupIntent
                )

            } else {

                /*
                 * Enkelttryk.
                 */
                prefs.edit()
                    .putLong(
                        "lastClickTime_$appWidgetId",
                        now
                    )
                    .apply()

                refresh(
                    context,
                    intArrayOf(
                        appWidgetId
                    )
                )
            }
        }
    }

    /*
     * ----------------------------------------
     * WIDGET SLETTES
     * ----------------------------------------
     *
     * Her rydder vi ALT op, som tilhører
     * den konkrete widget.
     */
    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray
    ) {

        super.onDeleted(
            context,
            appWidgetIds
        )

        val workManager =
            WorkManager.getInstance(
                context
            )

        val prefs =
            context.getSharedPreferences(
                "settings",
                Context.MODE_PRIVATE
            )

        appWidgetIds.forEach { id ->

            /*
             * Stop planlagte/oprettede jobs.
             */
            workManager.cancelUniqueWork(
                "html-widget-refresh-$id"
            )

            workManager.cancelUniqueWork(
                "website-refresh-$id"
            )

            /*
             * Fjern alle indstillinger og
             * debug-værdier for denne widget.
             */
            prefs.edit()

                /*
                 * Grundindstillinger.
                 */
                .remove(
                    "url_$id"
                )

                .remove(
                    "localUri_$id"
                )

                .remove(
                    "refreshMinutes_$id"
                )

                .remove(
                    "hideCookieBanners_$id"
                )

                /*
                 * Klik.
                 */
                .remove(
                    "lastClickTime_$id"
                )

                /*
                 * Seneste vellykkede opdatering.
                 */
                .remove(
                    "lastSuccessfulUpdate_$id"
                )

                /*
                 * Crop aktiveret.
                 */
                .remove(
                    "cropEnabled_$id"
                )

                /*
                 * CSS-koordinater.
                 */
                .remove(
                    "cropCssX_$id"
                )

                .remove(
                    "cropCssY_$id"
                )

                .remove(
                    "cropCssWidth_$id"
                )

                .remove(
                    "cropCssHeight_$id"
                )

                /*
                 * CSS viewport.
                 */
                .remove(
                    "cropCssViewportWidth_$id"
                )

                .remove(
                    "cropCssViewportHeight_$id"
                )

                .remove(
                    "cropCssDocumentWidth_$id"
                )

                .remove(
                    "cropCssDocumentHeight_$id"
                )

                /*
                 * Android/WebView viewport.
                 */
                .remove(
                    "cropViewportWidth_$id"
                )

                .remove(
                    "cropViewportHeight_$id"
                )

                .remove(
                    "cropWebScale_$id"
                )

                /*
                 * Valgt område.
                 */
                .remove(
                    "cropSelectionWidth_$id"
                )

                .remove(
                    "cropSelectionHeight_$id"
                )

                /*
                 * Scroll-position.
                 */
                .remove(
                    "websiteScrollX_$id"
                )

                .remove(
                    "websiteScrollY_$id"
                )

                /*
                 * Den røde rammes position
                 * og størrelse.
                 */
                .remove(
                    "cropFrameX_$id"
                )

                .remove(
                    "cropFrameY_$id"
                )

                .remove(
                    "cropFrameWidth_$id"
                )

                .remove(
                    "cropFrameHeight_$id"
                )

                /*
                 * Worker debug-data.
                 */
                .remove(
                    "cropBitmapX_$id"
                )

                .remove(
                    "cropBitmapY_$id"
                )

                .remove(
                    "cropWorkerCssViewportWidth_$id"
                )

                .remove(
                    "cropWorkerCssViewportHeight_$id"
                )

                .remove(
                    "cropWorkerPixelsPerCssX_$id"
                )

                .remove(
                    "cropWorkerPixelsPerCssY_$id"
                )

                /*
                 * Ældre crop-data.
                 *
                 * Disse kan ligge tilbage fra
                 * tidligere versioner af appen.
                 */
                .remove(
                    "cropDocumentX_$id"
                )

                .remove(
                    "cropDocumentY_$id"
                )

                .remove(
                    "cropDocumentSelectionWidth_$id"
                )

                .remove(
                    "cropDocumentSelectionHeight_$id"
                )

                .remove(
                    "cropWorkerWebScale_$id"
                )

                .remove(
                    "cropRelativeX_$id"
                )

                .remove(
                    "cropRelativeY_$id"
                )

                .remove(
                    "cropRelativeWidth_$id"
                )

                .remove(
                    "cropRelativeHeight_$id"
                )

                .remove(
                    "cropScrollX_$id"
                )

                .remove(
                    "cropScrollY_$id"
                )

                .remove(
                    "cropXRatio_$id"
                )

                .remove(
                    "cropYRatio_$id"
                )

                .remove(
                    "cropScale_$id"
                )

                .apply()
        }
    }

    companion object {

        private const val ACTION_REFRESH_NOW =
            "dk.htmlwidget.REFRESH_NOW"

        /*
         * ----------------------------------------
         * OPDATER WIDGET
         * ----------------------------------------
         */
        fun refresh(
            context: Context,
            ids: IntArray
        ) {

            if (
                ids.isEmpty()
            ) {
                return
            }

            val manager =
                AppWidgetManager.getInstance(
                    context
                )

            val prefs =
                context.getSharedPreferences(
                    "settings",
                    Context.MODE_PRIVATE
                )

            ids.forEach { id ->

                /*
                 * --------------------------------
                 * TRYK-HANDLING
                 * --------------------------------
                 */
                val clickIntent =
                    Intent(
                        context,
                        HtmlWidgetProvider::class.java
                    ).apply {

                        action =
                            ACTION_REFRESH_NOW

                        putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            id
                        )
                    }

                val clickPendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        id,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                    )

                val clickViews =
                    RemoteViews(
                        context.packageName,
                        R.layout.widget_layout
                    )

                clickViews
                    .setOnClickPendingIntent(
                        R.id.widget_image,
                        clickPendingIntent
                    )

                /*
                 * VIGTIGT:
                 *
                 * Vi ændrer kun click-handleren.
                 * Det eksisterende billede bliver
                 * stående, mens en ny version
                 * renderes.
                 */
                manager.updateAppWidget(
                    id,
                    clickViews
                )

                val website =
                    prefs.getString(
                        "url_$id",
                        null
                    )

                val localUri =
                    prefs.getString(
                        "localUri_$id",
                        null
                    )

                /*
                 * --------------------------------
                 * HJEMMESIDE
                 * --------------------------------
                 */
                if (
                    !website.isNullOrBlank()
                ) {

                    val request =
                        OneTimeWorkRequestBuilder<
                            WebsiteRefreshWorker
                        >()
                            .setInputData(
                                workDataOf(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID to
                                        id
                                )
                            )
                            .build()

                    WorkManager
                        .getInstance(
                            context
                        )
                        .enqueueUniqueWork(
                            "website-refresh-$id",
                            ExistingWorkPolicy.REPLACE,
                            request
                        )

                    return@forEach
                }

                /*
                 * --------------------------------
                 * LOKAL HTML
                 * --------------------------------
                 */
                if (
                    !localUri.isNullOrBlank()
                ) {

                    HtmlRenderService.start(
                        context,
                        id
                    )
                }
            }
        }
    }
}
