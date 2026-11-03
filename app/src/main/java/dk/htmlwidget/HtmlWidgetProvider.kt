package dk.htmlwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File

open class HtmlWidgetProvider :
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
     * Tryk på billedet:
     * Åbn hjemmesiden.
     *
     * Tryk på ↻:
     * Manuel opdatering.
     *
     * 2 tryk på ↻ inden for 800 ms:
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

        /*
         * ----------------------------------------
         * ÅBN HJEMMESIDE
         * ----------------------------------------
         */
        if (
            intent.action ==
            ACTION_OPEN_WEBSITE
        ) {

            val appWidgetId =
                intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )

            if (
                appWidgetId !=
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) {

                val prefs =
                    context.getSharedPreferences(
                        "settings",
                        Context.MODE_PRIVATE
                    )

                val website =
                    prefs.getString(
                        "url_$appWidgetId",
                        null
                    )

                if (
                    !website.isNullOrBlank()
                ) {

                    val openIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(website)
                        ).apply {

                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK
                        }

                    context.startActivity(
                        openIntent
                    )
                }
            }

            return
        }

        /*
         * ----------------------------------------
         * MANUEL OPDATERING / INDSTILLINGER
         * ----------------------------------------
         */
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

            try { File(context.filesDir, "widget_$id.png").delete() } catch (_: Exception) {}

            val editor =
                prefs.edit()

            editor
                .remove("url_$id")
                .remove("localUri_$id")
                .remove("widgetName_$id")
                .remove("refreshMinutes_$id")

                .remove(
                    "hideCookieBanners_$id"
                )

                .remove(
                    "showUpdateStatus_$id"
                )

                .remove(
                    "autoSwitchViews_$id"
                )

                .remove(
                    "roundedCorners_$id"
                )

                .remove(
                    "showBorder_$id"
                )

                .remove(
                    "showBackground_$id"
                )

                .remove(
                    "lastClickTime_$id"
                )

                .remove(
                    "lastSuccessfulUpdate_$id"
                )

                .remove(
                    "lastBitmapChecksum_$id"
                )

                .remove(
                    "currentCropView_$id"
                )

                .remove(
                    "cropViewCount_$id"
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

            /*
             * Fjern eventuelle gemte ekstra visninger.
             */
            for (
                viewNumber in 1..50
            ) {

                editor
                    .remove(
                        "cropView_${viewNumber}_cropCssX_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropCssY_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropCssWidth_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropCssHeight_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropCssViewportWidth_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropCssViewportHeight_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropCssDocumentWidth_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropCssDocumentHeight_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropViewportWidth_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropViewportHeight_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropWebScale_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropSelectionWidth_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropSelectionHeight_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropFrameWidth_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropFrameHeight_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_websiteScrollX_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_websiteScrollY_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropFrameX_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_cropFrameY_$id"
                    )
                    .remove(
                        "cropView_${viewNumber}_enabled_$id"
                    )
            }

            editor.apply()
        }
    }

    companion object {

        private const val ACTION_REFRESH_NOW =
            "dk.htmlwidget.REFRESH_NOW"

        private const val ACTION_OPEN_WEBSITE =
            "dk.htmlwidget.OPEN_WEBSITE"

        private fun restoreSavedImage(
            context: Context,
            appWidgetId: Int,
            manager: AppWidgetManager
        ) {
            try {
                val file = File(context.filesDir, "widget_$appWidgetId.png")
                if (!file.exists() || file.length() <= 0L) return
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                applyWidgetAppearance(context, appWidgetId, views)
                views.setImageViewBitmap(R.id.widget_image, bitmap)
                manager.partiallyUpdateAppWidget(appWidgetId, views)
            } catch (_: Exception) {
            }
        }

        /*
         * ----------------------------------------
         * ANVEND WIDGET-UDSEENDE
         * ----------------------------------------
         */
        private fun applyWidgetAppearance(
            context: Context,
            appWidgetId: Int,
            views: RemoteViews
        ) {

            val prefs =
                context.getSharedPreferences(
                    "settings",
                    Context.MODE_PRIVATE
                )

            val roundedCorners =
                prefs.getBoolean(
                    "roundedCorners_$appWidgetId",
                    true
                )

            val showBorder =
                prefs.getBoolean(
                    "showBorder_$appWidgetId",
                    false
                )

            val showBackground =
                prefs.getBoolean(
                    "showBackground_$appWidgetId",
                    true
                )

            /*
             * VIGTIGT:
             *
             * Vi styrer IKKE widget_border her.
             * Den ændring gav "Widget kan ikke vises"
             * på Android-launcheren.
             */

            val backgroundResource =
                when {

                    !showBackground ->
                        0

                    roundedCorners &&
                        showBorder ->
                        R.drawable.widget_background_rounded_border

                    roundedCorners ->
                        R.drawable.widget_background_rounded

                    showBorder ->
                        R.drawable.widget_background_border

                    else ->
                        0
                }

            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                backgroundResource
            )
        }

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

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.widget_layout
                )

            applyWidgetAppearance(
                context,
                appWidgetId,
                views
            )

            val language =
    prefs.getString(
        "appLanguage",
        "da"
    ) ?: "da"

val configuration =
    android.content.res.Configuration(
        context.resources.configuration
    )

configuration.setLocale(
    java.util.Locale(language)
)

val localizedContext =
    context.createConfigurationContext(
        configuration
    )

views.setTextViewText(
    R.id.widget_status,
    localizedContext.getString(
        R.string.status_updating
    )
)

            views.setViewVisibility(
                R.id.widget_status,
                View.VISIBLE
            )

            AppWidgetManager
                .getInstance(
                    context
                )
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

            applyWidgetAppearance(
                context,
                appWidgetId,
                views
            )

            views.setViewVisibility(
                R.id.widget_status,
                View.GONE
            )

            AppWidgetManager
                .getInstance(
                    context
                )
                .partiallyUpdateAppWidget(
                    appWidgetId,
                    views
                )
        }

        /*
         * ----------------------------------------
         * OPDATER
         * ----------------------------------------
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

                restoreSavedImage(context, id, manager)

                /*
                 * ↻ OPDATER / DOBBELTTRYK INDSTILLINGER
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

                /*
                 * TRYK PÅ BILLEDET = ÅBN HJEMMESIDE
                 */
                val openWebsiteIntent =
                    Intent(
                        context,
                        HtmlWidgetProvider::class.java
                    ).apply {

                        action =
                            ACTION_OPEN_WEBSITE

                        putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            id
                        )
                    }

                val openWebsitePendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        id + 100000,
                        openWebsiteIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                    )

                val clickViews =
                    RemoteViews(
                        context.packageName,
                        R.layout.widget_layout
                    )

                applyWidgetAppearance(
                    context,
                    id,
                    clickViews
                )

                clickViews
                    .setOnClickPendingIntent(
                        R.id.widget_image,
                        openWebsitePendingIntent
                    )

                clickViews
                    .setOnClickPendingIntent(
                        R.id.widget_refresh,
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
