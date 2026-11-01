package dk.htmlwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
            appWidgetIds,
            false
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
            intArrayOf(appWidgetId),
            false
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
     * Manuel opdatering.
     *
     * 2 tryk inden for 800 ms:
     * Åbn indstillinger.
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

                prefs.edit()
                    .remove(
                        "lastClickTime_$appWidgetId"
                    )
                    .apply()

                /*
                 * Hvis første tryk nåede at vise
                 * "Opdaterer…", skjuler vi teksten
                 * igen, når det viser sig at være
                 * et dobbelttryk.
                 */
                hideStatus(
                    context,
                    appWidgetId
                )

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

                prefs.edit()
                    .putLong(
                        "lastClickTime_$appWidgetId",
                        now
                    )
                    .apply()

                /*
                 * Manuel opdatering.
                 */
                refresh(
                    context,
                    intArrayOf(appWidgetId),
                    true
                )
            }
        }
    }

    /*
     * ----------------------------------------
     * WIDGET SLETTES
     * ----------------------------------------
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

            workManager.cancelUniqueWork(
                "html-widget-refresh-$id"
            )

            workManager.cancelUniqueWork(
                "website-refresh-$id"
            )

            prefs.edit()

                .remove("url_$id")
                .remove("localUri_$id")
                .remove("refreshMinutes_$id")

                .remove(
                    "hideCookieBanners_$id"
                )

                .remove(
                    "showUpdateStatus_$id"
                )

                .remove(
                    "lastClickTime_$id"
                )

                .remove(
                    "lastSuccessfulUpdate_$id"
                )

                .remove(
                    "cropEnabled_$id"
                )

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

                .remove(
                    "cropViewportWidth_$id"
                )

                .remove(
                    "cropViewportHeight_$id"
                )

                .remove(
                    "cropWebScale_$id"
                )

                .remove(
                    "cropSelectionWidth_$id"
                )

                .remove(
                    "cropSelectionHeight_$id"
                )

                .remove(
                    "websiteScrollX_$id"
                )

                .remove(
                    "websiteScrollY_$id"
                )

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
                 * Gamle værdier fra tidligere
                 * crop-versioner.
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
         * VIS "OPDATERER…"
         * ----------------------------------------
         */
        private fun showUpdating(
            context: Context,
            appWidgetId: Int
        ) {

            val prefs =
                context.getSharedPreferences(
                    "settings",
                    Context.MODE_PRIVATE
                )

            /*
             * Standard = slået til.
             *
             * I næste SetupActivity-fil får
             * brugeren en checkbox til dette.
             */
            val showStatus =
                prefs.getBoolean(
                    "showUpdateStatus_$appWidgetId",
                    true
                )

            if (!showStatus) {
                return
            }

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.widget_layout
                )

            views.setTextViewText(
                R.id.widget_status,
                "Opdaterer…"
            )

            views.setViewVisibility(
                R.id.widget_status,
                View.VISIBLE
            )

            AppWidgetManager
                .getInstance(context)
                .partiallyUpdateAppWidget(
                    appWidgetId,
                    views
                )
        }

        /*
         * ----------------------------------------
         * SKJUL STATUS
         * ----------------------------------------
         */
        private fun hideStatus(
            context: Context,
            appWidgetId: Int
        ) {

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.widget_layout
                )

            views.setViewVisibility(
                R.id.widget_status,
                View.GONE
            )

            AppWidgetManager
                .getInstance(context)
                .partiallyUpdateAppWidget(
                    appWidgetId,
                    views
                )
        }

        /*
         * ----------------------------------------
         * OPDATER
         * ----------------------------------------
         *
         * manualRefresh = true:
         * Vis "Opdaterer…".
         *
         * manualRefresh = false:
         * Baggrundsopdatering uden status.
         */
        fun refresh(
            context: Context,
            ids: IntArray,
            manualRefresh: Boolean = false
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
                 * Klik-handler.
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

                manager.partiallyUpdateAppWidget(
                    id,
                    clickViews
                )

                /*
                 * Ved manuel opdatering viser
                 * vi status oven på det gamle
                 * fungerende billede.
                 */
                if (
                    manualRefresh
                ) {

                    showUpdating(
                        context,
                        id
                    )
                }

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
                 * WEBSITE
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
                                        id,
                                    "manualRefresh" to
                                        manualRefresh
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
                 * LOKAL HTML
                 */
                if (
                    !localUri.isNullOrBlank()
                ) {

                    HtmlRenderService.start(
                        context,
                        id
                    )

                    return@forEach
                }

                /*
                 * Intet at opdatere.
                 */
                if (
                    manualRefresh
                ) {

                    hideStatus(
                        context,
                        id
                    )
                }
            }
        }
    }
}
