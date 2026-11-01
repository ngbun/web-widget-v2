package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class WidgetPreviewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    30,
                    30,
                    30,
                    30
                )
            }

        root.addView(
            TextView(this).apply {
                text = "Preview"
                textSize = 26f
            }
        )

        root.addView(
            TextView(this).apply {
                text =
                    "Sådan ser det senest gemte billede til widgetten ud."

                textSize = 14f

                setPadding(
                    0,
                    5,
                    0,
                    20
                )
            }
        )

        val preview =
            ImageView(this).apply {

                adjustViewBounds = true

                scaleType =
                    ImageView.ScaleType.FIT_CENTER
            }

        val imageFile =
            File(
                filesDir,
                "widget_$appWidgetId.png"
            )

        if (
            appWidgetId !=
            AppWidgetManager.INVALID_APPWIDGET_ID &&
            imageFile.exists()
        ) {

            val bitmap =
                BitmapFactory.decodeFile(
                    imageFile.absolutePath
                )

            if (bitmap != null) {

                preview.setImageBitmap(
                    bitmap
                )

                root.addView(
                    preview,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                )

            } else {

                showNoPreview(
                    root
                )
            }

        } else {

            showNoPreview(
                root
            )
        }

        setContentView(
            root
        )
    }

    private fun showNoPreview(
        root: LinearLayout
    ) {

        root.addView(
            TextView(this).apply {

                text =
                    "Der er endnu ikke et preview."

                textSize = 18f

                gravity =
                    Gravity.CENTER

                setPadding(
                    20,
                    80,
                    20,
                    80
                )
            }
        )
    }
}
