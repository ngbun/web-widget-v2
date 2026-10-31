package dk.htmlwidget

import android.app.Application
import android.webkit.WebView

class WebWidgetApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        WebView.enableSlowWholeDocumentDraw()
    }
}
