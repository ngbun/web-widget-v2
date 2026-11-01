package dk.htmlwidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class ScreenWakeReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        /*
         * Reager kun når skærmen tændes.
         */
        if (
            intent.action !=
            Intent.ACTION_SCREEN_ON
        ) {
            return
        }

        val appWidgetManager =
            AppWidgetManager.getInstance(
                context
            )

        /*
         * Find alle aktive widgets
         * fra vores provider.
         */
        val widgetIds =
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    HtmlWidgetProvider::class.java
                )
            )

        if (
            widgetIds.isEmpty()
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

        /*
         * Minimum 5 minutter mellem
         * wake-opdateringer.
         */
        val minimumAge =
            5L * 60L * 1000L

        val widgetsToRefresh =
            widgetIds.filter { id ->

                val lastSuccessfulUpdate =
                    prefs.getLong(
                        "lastSuccessfulUpdate_$id",
                        0L
                    )

                /*
                 * Hvis widgetten aldrig tidligere
                 * er blevet opdateret, tillader vi
                 * også en opdatering.
                 */
                lastSuccessfulUpdate == 0L ||
                    now - lastSuccessfulUpdate >=
                    minimumAge
            }

        if (
            widgetsToRefresh.isEmpty()
        ) {
            return
        }

        /*
         * Brug det eksisterende refresh-system.
         *
         * false = automatisk opdatering,
         * så "Opdaterer..." ikke vises.
         */
        HtmlWidgetProvider.refresh(
            context,
            widgetsToRefresh.toIntArray(),
            false
        )
    }
}
