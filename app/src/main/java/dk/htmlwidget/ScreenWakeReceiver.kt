package dk.htmlwidget

import android.content.BroadcastReceiver
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

        /*
         * Selve widget-opdateringen kobler vi på
         * i næste trin.
         *
         * Først sikrer vi, at receiveren bygger
         * uden at ændre noget i den fungerende app.
         */
    }
}
