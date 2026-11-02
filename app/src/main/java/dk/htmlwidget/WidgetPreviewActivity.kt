package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.util.Locale

class WidgetPreviewActivity : Activity() {

    private fun text(
        resId: Int,
        vararg formatArgs: Any
    ): String {

        val prefs =
            getSharedPreferences(
                "settings",
                MODE_PRIVATE
            )

        val language =
            prefs.getString(
                "appLanguage",
                "da"
            ) ?: "da"

        val configuration =
            Configuration(
                resources.configuration
            )

        configuration.setLocale(
            Locale(language)
        )

        val localizedContext: Context =
            createConfigurationContext(
                configuration
            )

        return if (
            formatArgs.isEmpty()
        ) {

            localizedContext.getString(
                resId
            )

        } else {

            localizedContext.getString(
                resId,
                *formatArgs
            )
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        val appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

        val prefs =
            getSharedPreferences(
                "settings",
                MODE_PRIVATE
            )

        val widgetName =
            if (
                appWidgetId !=
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) {

                prefs.getString(
                    "widgetName_$appWidgetId",
                    ""
                ).orEmpty()

            } else {

                ""
            }

        val roundedCorners =
            if (
                appWidgetId !=
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) {

                prefs.getBoolean(
                    "roundedCorners_$appWidgetId",
                    true
                )

            } else {

                true
            }

        val showBorder =
            if (
                appWidgetId !=
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) {

                prefs.getBoolean(
                    "showBorder_$appWidgetId",
                    false
                )

            } else {

                false
            }

        val showBackground =
            if (
                appWidgetId !=
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) {

                prefs.getBoolean(
                    "showBackground_$appWidgetId",
                    true
                )

            } else {

                true
            }

        val density =
            resources.displayMetrics.density

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    (24 * density).toInt(),
                    (24 * density).toInt(),
                    (24 * density).toInt(),
                    (24 * density).toInt()
                )
            }

        /*
         * OVERSKRIFT
         */
        root.addView(
            TextView(this).apply {

                text =
                    if (
                        widgetName.isNotBlank()
                    ) {

                        text(
                            R.string.preview_title_named,
                            widgetName
                        )

                    } else {

                        text(
                            R.string.preview_title
                        )
                    }

                textSize = 26f
            }
        )

        /*
         * BESKRIVELSE
         */
        root.addView(
            TextView(this).apply {

                text =
                    text(
                        R.string.preview_description
                    )

                textSize = 14f

                setPadding(
                    0,
                    (5 * density).toInt(),
                    0,
                    (20 * density).toInt()
                )
            }
        )

        /*
         * PREVIEW CONTAINER
         */
        val previewContainer =
            FrameLayout(this).apply {

                val shape =
                    GradientDrawable()

                shape.shape =
                    GradientDrawable.RECTANGLE

                shape.cornerRadius =
                    if (
                        roundedCorners
                    ) {

                        18f * density

                    } else {

                        0f
                    }

                if (
                    showBackground
                ) {

                    shape.setColor(
                        Color.WHITE
                    )

                } else {

                    shape.setColor(
                        Color.TRANSPARENT
                    )
                }

                if (
                    showBorder
                ) {

                    shape.setStroke(
                        (1 * density)
                            .toInt()
                            .coerceAtLeast(1),
                        0x40000000
                            .toInt()
                    )
                }

                background =
                    shape

                if (
                    roundedCorners
                ) {

                    clipToOutline = true
                }
            }

        val preview =
            ImageView(this).apply {

                scaleType =
                    ImageView.ScaleType.FIT_XY

                adjustViewBounds =
                    false
            }

        previewContainer.addView(
            preview,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        /*
         * HENT SENEST GEMTE BILLEDE
         */
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

            if (
                bitmap != null
            ) {

                preview.setImageBitmap(
                    bitmap
                )

                root.addView(
                    previewContainer,
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
                    text(
                        R.string.preview_none
                    )

                textSize =
                    18f

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
