package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import kotlin.math.roundToInt

class CropWebsiteActivity : Activity() {

    private var appWidgetId =
        AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            finish()
            return
        }

        val previewFile =
            File(
                cacheDir,
                "crop_preview_$appWidgetId.png"
            )

        if (!previewFile.exists()) {

            Toast.makeText(
                this,
                "Opdater widgetten først, og prøv derefter igen.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val previewBitmap =
            BitmapFactory.decodeFile(
                previewFile.absolutePath
            )

        if (previewBitmap == null) {

            Toast.makeText(
                this,
                "Kunne ikke åbne forhåndsvisningen.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val manager =
            AppWidgetManager.getInstance(this)

        val options =
            manager.getAppWidgetOptions(
                appWidgetId
            )

        val density =
            resources.displayMetrics.density

        val widgetWidth = (
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                300
            ) * density
        ).roundToInt()
            .coerceAtLeast(1)

        val widgetHeight = (
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                180
            ) * density
        ).roundToInt()
            .coerceAtLeast(1)

        val layout =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        layout.addView(
            TextView(this).apply {

                text =
                    "Flyt den røde ramme til det område, der skal vises i widgetten."

                textSize = 16f

                setPadding(
                    30,
                    20,
                    30,
                    20
                )
            }
        )

        val cropView =
            CropPreviewView(
                previewBitmap,
                widgetWidth,
                widgetHeight
            )

        layout.addView(
            cropView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        layout.addView(
            Button(this).apply {

                text =
                    "Gem dette område"

                setOnClickListener {

                    val prefs =
                        getSharedPreferences(
                            "settings",
                            MODE_PRIVATE
                        )

                    prefs.edit()
                        .putInt(
                            "cropBitmapX_$appWidgetId",
                            cropView.cropX
                        )
                        .putInt(
                            "cropBitmapY_$appWidgetId",
                            cropView.cropY
                        )
                        .putBoolean(
                            "cropEnabled_$appWidgetId",
                            true
                        )
                        .apply()

                    Toast.makeText(
                        this@CropWebsiteActivity,
                        "Området er gemt",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            }
        )

        setContentView(layout)
    }

    private inner class CropPreviewView(
        private val bitmap: Bitmap,
        private val cropWidth: Int,
        private val cropHeight: Int
    ) : View(this) {

        var cropX = 0
            private set

        var cropY = 0
            private set

        private var displayScale =
            1f

        private var displayOffsetX =
            0f

        private var displayOffsetY =
            0f

        private var lastTouchX =
            0f

        private var lastTouchY =
            0f

        private val bitmapPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
            )

        private val framePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                style =
                    Paint.Style.STROKE

                strokeWidth =
                    6f

                color =
                    android.graphics.Color.RED
            }

        private val shadePaint =
            Paint().apply {

                color =
                    android.graphics.Color.argb(
                        110,
                        0,
                        0,
                        0
                    )
            }

        override fun onDraw(
            canvas: Canvas
        ) {
            super.onDraw(canvas)

            if (
                width <= 0 ||
                height <= 0
            ) {
                return
            }

            displayScale =
                minOf(
                    width.toFloat() /
                        bitmap.width.toFloat(),

                    height.toFloat() /
                        bitmap.height.toFloat()
                )

            val shownWidth =
                bitmap.width *
                    displayScale

            val shownHeight =
                bitmap.height *
                    displayScale

            displayOffsetX =
                (width - shownWidth) /
                    2f

            displayOffsetY =
                (height - shownHeight) /
                    2f

            val destination =
                RectF(
                    displayOffsetX,
                    displayOffsetY,
                    displayOffsetX +
                        shownWidth,
                    displayOffsetY +
                        shownHeight
                )

            canvas.drawBitmap(
                bitmap,
                null,
                destination,
                bitmapPaint
            )

            val left =
                displayOffsetX +
                    cropX *
                    displayScale

            val top =
                displayOffsetY +
                    cropY *
                    displayScale

            val right =
                left +
                    cropWidth *
                    displayScale

            val bottom =
                top +
                    cropHeight *
                    displayScale

            /*
             * Gør området uden for widgetrammen mørkere.
             */
            canvas.drawRect(
                displayOffsetX,
                displayOffsetY,
                displayOffsetX + shownWidth,
                top,
                shadePaint
            )

            canvas.drawRect(
                displayOffsetX,
                bottom,
                displayOffsetX + shownWidth,
                displayOffsetY + shownHeight,
                shadePaint
            )

            canvas.drawRect(
                displayOffsetX,
                top,
                left,
                bottom,
                shadePaint
            )

            canvas.drawRect(
                right,
                top,
                displayOffsetX + shownWidth,
                bottom,
                shadePaint
            )

            canvas.drawRect(
                left,
                top,
                right,
                bottom,
                framePaint
            )
        }

        override fun onTouchEvent(
            event: MotionEvent
        ): Boolean {

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    lastTouchX =
                        event.x

                    lastTouchY =
                        event.y

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    if (displayScale <= 0f) {
                        return true
                    }

                    val deltaX =
                        (
                            (event.x - lastTouchX) /
                                displayScale
                        ).roundToInt()

                    val deltaY =
                        (
                            (event.y - lastTouchY) /
                                displayScale
                        ).roundToInt()

                    val maxX =
                        (
                            bitmap.width -
                                cropWidth
                            ).coerceAtLeast(0)

                    val maxY =
                        (
                            bitmap.height -
                                cropHeight
                            ).coerceAtLeast(0)

                    cropX =
                        (cropX + deltaX)
                            .coerceIn(
                                0,
                                maxX
                            )

                    cropY =
                        (cropY + deltaY)
                            .coerceIn(
                                0,
                                maxY
                            )

                    lastTouchX =
                        event.x

                    lastTouchY =
                        event.y

                    invalidate()

                    return true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    performClick()
                    return true
                }
            }

            return super.onTouchEvent(
                event
            )
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }
}
