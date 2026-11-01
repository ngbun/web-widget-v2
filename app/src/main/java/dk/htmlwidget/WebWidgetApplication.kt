package dk.htmlwidget

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.webkit.WebView

class WebWidgetApplication : Application() {

    private lateinit var screenWakeReceiver:
        ScreenWakeReceiver

    override fun onCreate() {

        super.onCreate()

        /*
         * Beholder vores eksisterende
         * WebView-indstilling.
         */
        WebView.enableSlowWholeDocumentDraw()

        /*
         * Registrer receiveren dynamisk.
         *
         * Når app-processen lever, kan vi dermed
         * reagere på at skærmen bliver tændt.
         */
        screenWakeReceiver =
            ScreenWakeReceiver()

        registerReceiver(
            screenWakeReceiver,
            IntentFilter(
                Intent.ACTION_SCREEN_ON
            )
        )
    }
}
