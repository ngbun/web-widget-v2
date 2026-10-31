package dk.htmlwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews

class HtmlWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refresh(context, appWidgetIds)
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
            intArrayOf(appWidgetId)
        )
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }
override fun onDeleted(
    context: Context,
    appWidgetIds: IntArray
) {
    super.onDeleted(context, appWidgetIds)

    val workManager =
        androidx.work.WorkManager.getInstance(context)

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
            .apply()
    }
}
    companion object {

        fun refresh(
            context: Context,
            ids: IntArray
        ) {
            if (ids.isEmpty()) return

            val manager =
                AppWidgetManager.getInstance(context)

            val prefs =
                context.getSharedPreferences(
                    "settings",
                    Context.MODE_PRIVATE
                )

            ids.forEach { id ->

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
                 * HJEMMESIDE
                 *
                 * Vi overskriver IKKE billedet.
                 * Det eksisterende billede bliver stående.
                 *
                 * Et tryk på widgetten åbner
                 * WebsiteRenderActivity.
                 */
                
                

                if (!website.isNullOrBlank()) {

    val request =
        androidx.work.OneTimeWorkRequestBuilder<WebsiteRefreshWorker>()
            .setInputData(
                androidx.work.workDataOf(
                    AppWidgetManager.EXTRA_APPWIDGET_ID to id
                )
            )
            .build()

    androidx.work.WorkManager
        .getInstance(context)
        .enqueueUniqueWork(
            "website-refresh-$id",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )

    return@forEach
                }
                /*
                 * LOKAL HTML
                 */
                if (!localUri.isNullOrBlank()) {

                    HtmlRenderService.start(
                        context,
                        id
                    )
                }
            }
        }
    }
}
